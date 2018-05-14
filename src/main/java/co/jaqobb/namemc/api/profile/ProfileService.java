/*
 * MIT License
 *
 * Copyright (c) 2018 Jakub Zagórski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package co.jaqobb.namemc.api.profile;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import co.jaqobb.namemc.api.json.JSONArray;
import co.jaqobb.namemc.api.util.IOUtils;

/**
 * Class used to store cached {@code Profile},
 * and to cache new ones.
 */
public class ProfileService {
	/**
	 * Creates new {@code ProfileService} instance
	 * with the default values being 5 as a time
	 * and minutes, as a time unit.
	 */
	public static ProfileService newDefault() {
		return new ProfileService();
	}

	/**
	 * Creates new {@code ProfileService} instance
	 * with the given time and time unit.
	 *
	 * @param time a time.
	 * @param unit a time unit.
	 */
	public static ProfileService newCustom(long time, TimeUnit unit) {
		return new ProfileService(time, unit);
	}

	/**
	 * Url used to cache {@code Profile}.
	 */
	private static final String PROFILE_FRIENDS_URL = "https://api.namemc.com/profile/%s/friends";

	/**
	 * Counter used to determine thread number.
	 */
	private static final AtomicInteger EXECUTOR_THREAD_COUNTER = new AtomicInteger();
	/**
	 * Executor used to cache {@code Profile}.
	 */
	private static final Executor EXECUTOR = Executors.newCachedThreadPool(runnable -> new Thread(runnable, "NameMC API Profile Query #" + EXECUTOR_THREAD_COUNTER.getAndIncrement()));

	/**
	 * Time that indicates whenever profile should be recached.
	 */
	private final long time;
	/**
	 * Time unit used to describe a unit of {@code time}.
	 */
	private final TimeUnit unit;
	/**
	 * Collection of currently cached profiles.
	 */
	private final Map<UUID, Profile> cache = Collections.synchronizedMap(new WeakHashMap<>(100));

	/**
	 * Creates new {@code ProfileService} instance
	 * with the default values being 5 as a time
	 * and minutes, as a time unit.
	 */
	private ProfileService() {
		this(5, TimeUnit.MINUTES);
	}

	/**
	 * Creates new {@code ProfileService} instance
	 * with the given time and time unit.
	 *
	 * @param time a time.
	 * @param unit a time unit.
	 */
	private ProfileService(long time, TimeUnit unit) {
		this.time = time;
		this.unit = unit;
	}

	/**
	 * Returns a collection of
	 * currently cached profiles.
	 *
	 * @return a collection of currently cached profiles.
	 */
	public Collection<Profile> all() {
		return Collections.unmodifiableCollection(this.cache.values());
	}

	/**
	 * Delegates cached {@code Profile} or
	 * caches new {@code Profile} with the
	 * given unique id and then delegates
	 * it to the {@code callback}.
	 *
	 * @param uniqueId a unique id to cache.
	 * @param recache  a state which defines
	 *                 if the recache should
	 *                 be forced.
	 * @param callback a callback where cached
	 *                 {@code Profile} and exception
	 *                 (null if everything went good)
	 *                 will be delegated to.
	 */
	public void lookup(UUID uniqueId, boolean recache, BiConsumer<Profile, Exception> callback) {
		synchronized (this.cache) {
			Profile profile = this.cache.get(uniqueId);
			if (profile != null && System.currentTimeMillis() - profile.getCacheTime() < this.unit.toMillis(this.time) && !recache) {
				callback.accept(profile, null);
				return;
			}
		}
		EXECUTOR.execute(() -> {
			String url = String.format(PROFILE_FRIENDS_URL, uniqueId.toString());
			try {
				String content = IOUtils.getWebsiteContent(url);
				JSONArray array = new JSONArray(content);
				Profile profile = new Profile(uniqueId, array);
				this.cache.put(uniqueId, profile);
				callback.accept(profile, null);
			} catch (IOException exception) {
				callback.accept(null, exception);
			}
		});
	}
}