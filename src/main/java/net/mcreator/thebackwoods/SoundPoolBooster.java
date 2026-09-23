package net.mcreator.thebackwoods; // Replace with your actual mod package name

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

@EventBusSubscriber(modid = "the_backwoods", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SoundPoolBooster {

    public static boolean ENABLED = true;
    public static int STATIC_LIMIT = 2048;
    public static int STREAMING_LIMIT = 512;

    public static boolean checkEnabled() {
        try {
            if (net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("rsls")) {
                System.out.println("[SoundBooster] Mod 'rsls' (Raise Sound Limit Simplified) is loaded! Automatically disabling SoundPoolBooster to prevent conflicts.");
                return false;
            }
        } catch (Throwable t) {
            // ModList might not be loaded yet in static initializer, which is fine
        }
        return true;
    }

    static {
        ENABLED = checkEnabled();
        if (ENABLED) {
            System.out.println("[SoundBooster] SoundPoolBooster class loaded! Attempting immediate static classload-time boost...");
            createAlsoftConfig();
            applyBoost();
        } else {
            System.out.println("[SoundBooster] SoundPoolBooster is DISABLED (conflict with 'rsls'). Skipping immediate boost.");
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ENABLED = checkEnabled();
            if (!ENABLED) {
                System.out.println("[SoundBooster] SoundPoolBooster is disabled (conflict with 'rsls'). Skipping setup boost.");
                return;
            }
            System.out.println("[SoundBooster] FMLClientSetupEvent triggered! Enforcing boost on thread queue...");
            createAlsoftConfig();
            applyBoost();
        });
    }

    public static void createAlsoftConfig() {
        if (!ENABLED) return;
        try {
            String configContent = "[general]\nsources = " + STATIC_LIMIT + "\nslots = 256\n";
            byte[] bytes = configContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 1. Current Working Directory (CWD)
            writeConfigSafe(new java.io.File("alsoft.ini"), bytes);
            writeConfigSafe(new java.io.File("alsoft.conf"), bytes);

            // 2. config/ folder (CWD/config)
            writeConfigSafe(new java.io.File("config/alsoft.ini"), bytes);
            writeConfigSafe(new java.io.File("config/alsoft.conf"), bytes);

            // 3. APPDATA (Windows)
            String appdata = System.getenv("APPDATA");
            if (appdata != null && !appdata.isEmpty()) {
                writeConfigSafe(new java.io.File(appdata, "alsoft.ini"), bytes);
            }

            // 4. User Home directory (All OS)
            String home = System.getProperty("user.home");
            if (home != null && !home.isEmpty()) {
                // Windows-style in home
                writeConfigSafe(new java.io.File(home, "alsoft.ini"), bytes);
                // Unix-style hidden in home
                writeConfigSafe(new java.io.File(home, ".alsoft.conf"), bytes);
                // Unix-style .config/alsoft.conf
                java.io.File nixConfigDir = new java.io.File(home, ".config");
                if (!nixConfigDir.exists()) {
                    nixConfigDir.mkdirs();
                }
                writeConfigSafe(new java.io.File(nixConfigDir, "alsoft.conf"), bytes);
                // macOS Library/Preferences/alsoft.conf
                java.io.File macPrefsDir = new java.io.File(home, "Library/Preferences");
                if (!macPrefsDir.exists()) {
                    macPrefsDir.mkdirs();
                }
                writeConfigSafe(new java.io.File(macPrefsDir, "alsoft.conf"), bytes);
            }

            // Set environment variables using programmatic modification
            java.io.File cwdFile = new java.io.File("alsoft.ini");
            setEnv("ALSOFT_CONF", cwdFile.getAbsolutePath());
            setEnv("ALSOFT_NUM_SOURCES", String.valueOf(STATIC_LIMIT));

            System.out.println("[SoundBooster] OpenAL Soft configuration successfully deployed across multiple potential target paths.");
        } catch (Exception e) {
            System.err.println("[SoundBooster] Error deploying config: " + e.getMessage());
        }
    }

    private static void writeConfigSafe(java.io.File file, byte[] bytes) {
        try {
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.write(file.toPath(), bytes);
            System.out.println("[SoundBooster] Deployed config to: " + file.getAbsolutePath());
        } catch (Exception ignored) {
            // Silently ignore write errors on restricted paths
        }
    }

    private static void setEnv(String key, String value) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            
            // Try Windows environment map
            try {
                Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
                theEnvironmentField.setAccessible(true);
                Object obj = theEnvironmentField.get(null);
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> env = (java.util.Map<String, String>) obj;
                    env.put(key, value);
                }
            } catch (Throwable ignored) {}

            try {
                Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
                theCaseInsensitiveEnvironmentField.setAccessible(true);
                Object obj = theCaseInsensitiveEnvironmentField.get(null);
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> env = (java.util.Map<String, String>) obj;
                    env.put(key, value);
                }
            } catch (Throwable ignored) {}

            // Try Unix (Linux/macOS) environment map
            try {
                java.util.Map<String, String> env = System.getenv();
                Class<?> cl = env.getClass();
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> writableEnv = (java.util.Map<String, String>) obj;
                    writableEnv.put(key, value);
                }
            } catch (Throwable ignored) {}
            
        } catch (Throwable t) {
            System.out.println("[SoundBooster] Could not modify System.getenv() via reflection: " + t.getMessage());
        }
    }

    public static void applyBoost() {
        if (!ENABLED) return;
        try {
            // Get the private limit field in Library.Pool (inner enum of Library)
            Field limitField = com.mojang.blaze3d.audio.Library.Pool.class.getDeclaredField("limit");
            limitField.setAccessible(true);

            int newStaticLimit = STATIC_LIMIT; 
            int newStreamingLimit = STREAMING_LIMIT;

            try {
                // Try standard reflection modification first
                limitField.setInt(com.mojang.blaze3d.audio.Library.Pool.STATIC, newStaticLimit);
                limitField.setInt(com.mojang.blaze3d.audio.Library.Pool.STREAMING, newStreamingLimit);
                System.out.println("[SoundBooster] Sound channels boosted successfully via Reflection! STATIC=" + newStaticLimit + ", STREAMING=" + newStreamingLimit);
            } catch (Exception reflectionError) {
                // Fallback: Bypass Java 21 final field restrictions using Unsafe
                Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Unsafe unsafe = (Unsafe) theUnsafeField.get(null);

                long offset = unsafe.objectFieldOffset(limitField);
                unsafe.putInt(com.mojang.blaze3d.audio.Library.Pool.STATIC, offset, newStaticLimit);
                unsafe.putInt(com.mojang.blaze3d.audio.Library.Pool.STREAMING, offset, newStreamingLimit);
                System.out.println("[SoundBooster] Sound channels boosted successfully via Unsafe! STATIC=" + newStaticLimit + ", STREAMING=" + newStreamingLimit);
            }
        } catch (Exception e) {
            System.err.println("[SoundBooster] Failed to modify sound pool limits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void applyActivePoolBoost(Object soundEngine) {
        if (!ENABLED) return;
        if (soundEngine == null) return;
        System.out.println("[SoundBooster] Injecting custom pool limits into active SoundEngine...");
        try {
            Field libraryField = null;
            try {
                libraryField = soundEngine.getClass().getDeclaredField("library");
            } catch (NoSuchFieldException e) {
                for (Field f : soundEngine.getClass().getDeclaredFields()) {
                    if (f.getType().getName().equals("com.mojang.blaze3d.audio.Library")) {
                        libraryField = f;
                        break;
                    }
                }
            }

            if (libraryField == null) {
                System.err.println("[SoundBooster] Could not find library field in SoundEngine!");
                return;
            }

            libraryField.setAccessible(true);
            Object library = libraryField.get(soundEngine);
            if (library == null) {
                System.err.println("[SoundBooster] Library instance in SoundEngine is null!");
                return;
            }

            Field staticPoolField = null;
            Field streamingPoolField = null;

            for (Field f : library.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("ChannelPool") || f.getType().getName().contains("Pool")) {
                    if (staticPoolField == null) {
                        staticPoolField = f;
                    } else if (streamingPoolField == null) {
                        streamingPoolField = f;
                        break;
                    }
                }
            }

            if (staticPoolField == null) {
                try {
                    staticPoolField = library.getClass().getDeclaredField("staticPool");
                } catch (NoSuchFieldException ignored) {}
            }
            if (streamingPoolField == null) {
                try {
                    streamingPoolField = library.getClass().getDeclaredField("streamingPool");
                } catch (NoSuchFieldException ignored) {}
            }

            if (staticPoolField == null || streamingPoolField == null) {
                System.err.println("[SoundBooster] staticPool or streamingPool fields not found in Library!");
                return;
            }

            staticPoolField.setAccessible(true);
            streamingPoolField.setAccessible(true);

            Object staticPool = staticPoolField.get(library);
            Object streamingPool = streamingPoolField.get(library);

            if (staticPool == null || streamingPool == null) {
                System.err.println("[SoundBooster] staticPool or streamingPool instance is null!");
                return;
            }

            int newStaticLimit = STATIC_LIMIT;
            int newStreamingLimit = STREAMING_LIMIT;

            setPoolLimit(staticPool, newStaticLimit);
            setPoolLimit(streamingPool, newStreamingLimit);

            System.out.println("[SoundBooster] Active Sound Pool limits successfully boosted in running instance! STATIC=" + newStaticLimit + ", STREAMING=" + newStreamingLimit);

        } catch (Exception e) {
            System.err.println("[SoundBooster] Error applying active pool boost: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setPoolLimit(Object pool, int limitValue) {
        try {
            Field limitField = null;
            try {
                limitField = pool.getClass().getDeclaredField("limit");
            } catch (NoSuchFieldException e) {
                for (Field f : pool.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class || f.getType() == Integer.TYPE) {
                        limitField = f;
                        break;
                    }
                }
            }

            if (limitField == null) {
                System.err.println("[SoundBooster] Could not find limit field in ChannelPool!");
                return;
            }

            limitField.setAccessible(true);
            try {
                limitField.setInt(pool, limitValue);
            } catch (Exception reflectionError) {
                Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
                long offset = unsafe.objectFieldOffset(limitField);
                unsafe.putInt(pool, offset, limitValue);
            }
        } catch (Exception e) {
            System.err.println("[SoundBooster] Failed to set pool limit: " + e.getMessage());
        }
    }

    @EventBusSubscriber(modid = "the_backwoods", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameBusListener {
        @SubscribeEvent
        public static void onSoundEngineLoad(SoundEngineLoadEvent event) {
            ENABLED = checkEnabled();
            if (!ENABLED) {
                System.out.println("[SoundBooster] SoundPoolBooster is disabled (conflict with 'rsls'). Skipping active pool boost.");
                return;
            }
            System.out.println("[SoundBooster] SoundEngineLoadEvent triggered! Applying pool boost...");
            applyActivePoolBoost(event.getEngine());
        }
    }
}
