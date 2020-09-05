package top.fols.aapp.xp.tstorage.hok.util.preference;

import android.content.SharedPreferences;
import android.util.Log;


import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import top.fols.aapp.xp.tstorage.hok.util.XposedDataStorage;
import top.fols.box.io.XStream;

/**
 * Encounter permission issues will automatically set all permissions
 */
public class XDXSharedPreferencesImpl implements SharedPreferences {
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners =
            new WeakHashMap<OnSharedPreferenceChangeListener, Object>();
    private static final Object CONTENT = new Object();
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mLock) {
            mListeners.put(listener, CONTENT);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mLock) {
            mListeners.remove(listener);
        }
    }


    private static final String TAG = "XSharedPreferencesImpl";

    private final Object mLock = new Object();
    private final XposedDataStorage xdx;
    private final File mFile;
    private Map<String, Object> mMap;

    public XDXSharedPreferencesImpl(XposedDataStorage xdp, File file) {
        this.xdx = xdp;
        this.mFile = file;
        this.loadFromDisk();
    }

    private void loadFromDisk() {
        // Debugging

        boolean canRead = false;
        if (!(canRead = xdx.canReadAbsoluteFile(this.mFile))) {
            Log.w(TAG, "Attempt to read preferences file " + mFile + " without permission");
        }

        if (canRead) {
            try {
                InputStream str = null;
                try {
                    str = xdx.getAbsolutePathFileRandomInputStream(this.mFile);
                    Map<String, Object> map = (Map<String, Object>) XDXXmlUtils.readMapXml(str);
                    this.mMap = map;
                } finally {
                    XStream.tryClose(str);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            this.mMap = new HashMap<>();
        }
    }


    public XDXSharedPreferencesImpl reload() {
        this.loadFromDisk();
        return this;
    }


    @Override
    public Map<String, ?> getAll() {
        synchronized (mLock) {
            //noinspection unchecked
            return new HashMap<String, Object>(this.mMap);
        }
    }


    @Override
    public String getString(String key, String defValue) {
        synchronized (mLock) {
            String v = (String)this.mMap.get(key);
            return v != null ? v : defValue;
        }
    }


    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        synchronized (mLock) {
            Set<String> v = (Set<String>) this.mMap.get(key);
            return v != null ? v : defValues;
        }
    }


    @Override
    public int getInt(String key, int defValue) {
        synchronized (mLock) {
            Integer v = (Integer)this.mMap.get(key);
            return v != null ? v : defValue;
        }
    }


    @Override
    public long getLong(String key, long defValue) {
        synchronized (mLock) {
            Long v = (Long)this.mMap.get(key);
            return v != null ? v : defValue;
        }
    }


    @Override
    public float getFloat(String key, float defValue) {
        synchronized (mLock) {
            Float v = (Float)this.mMap.get(key);
            return v != null ? v : defValue;
        }
    }


    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (mLock) {
            Boolean v = (Boolean)this.mMap.get(key);
            return v != null ? v : defValue;
        }
    }


    @Override
    public boolean contains(String key) {
        synchronized (mLock) {
            return this.mMap.containsKey(key);
        }
    }



    @Override
    public SharedPreferences.Editor edit() {
        // TODO: Implement this method
        return new Editor(this.xdx, this, this.mFile, this.mMap);
    }




    public static class Editor implements SharedPreferences.Editor {
        private XposedDataStorage mSuperXDS;
        private XDXSharedPreferencesImpl mSuper;
        private File mFile;
        private Map<String, Object> mMap;
        private Object mLock = new Object();
        private Editor(XposedDataStorage mSuperXDS, XDXSharedPreferencesImpl superObject, File file, Map<String, Object> map) {
            this.mSuperXDS = mSuperXDS;
            this.mSuper = superObject;
            this.mFile = file;
            this.mMap = map;
        }

        public Editor reload() {
            this.mSuper.loadFromDisk();
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            synchronized (mLock) {
                this.mMap.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            synchronized (mLock) {
                this.mMap.put(key,
                        (values == null) ? null : new HashSet<String>(values));
                return this;
            }
        }

        @Override
        public Editor putInt(String key, int value) {
            synchronized (mLock) {
                this.mMap.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putLong(String key, long value) {
            synchronized (mLock) {
                this.mMap.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putFloat(String key, float value) {
            synchronized (mLock) {
                this.mMap.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            synchronized (mLock) {
                this.mMap.put(key, value);
                return this;
            }
        }

        @Override
        public Editor remove(String key) {
            synchronized (mLock) {
                this.mMap.put(key, this);
                return this;
            }
        }

        @Override
        public Editor clear() {
            synchronized (mLock) {
                this.mMap.clear();
                return this;
            }
        }

        @Override
        public void apply() {
            try {
                OutputStream str = null;
                try {
                    str = this.mSuperXDS.getAbsolutePathFileRandomOutputStream(this.mFile);
                    XDXXmlUtils.writeMapXml(this.mMap, str);
                } finally {
                    XStream.tryClose(str);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (XmlPullParserException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean commit() {
            // TODO: Implement this method
            this.apply();
            return true;
        }
    }
}


