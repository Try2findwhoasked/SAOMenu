package com.sao.saomenu.client;

import dev.architectury.platform.Platform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;
import java.util.List;

/** Versioned soft bridge; the menu stores no authoritative RPG state. */
public final class SAORewriteBridge {
    private static final String API = "cn.saomod.rewrite.platform.client.integration.saomenu.SaoMenuClientApi";
    private static boolean reported;
    private SAORewriteBridge() { }
    public static int version() { return 1; }
    public static boolean installed() { return Platform.isModLoaded("saomod_rewrite"); }
    private static Object call(String method, Class<?>[] types, Object... args) {
        if (!installed()) return null;
        try {
            Class<?> api = Class.forName(API);
            if (!Integer.valueOf(version()).equals(api.getMethod("version").invoke(null))) {
                throw new IllegalStateException("Unsupported rewrite bridge version");
            }
            Method function = api.getMethod(method, types);
            return function.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            if (!reported) {
                reported = true;
                org.slf4j.LoggerFactory.getLogger("saomenu.rewrite").warn(
                        "RPG bridge unavailable; use rewrite screens and commands", failure);
            }
            return null;
        }
    }
    public static int level(Player player) {
        Object result = call("level", new Class<?>[] {Player.class}, player);
        return result instanceof Integer value ? value : -1;
    }
    @SuppressWarnings("unchecked")
    private static List<String> rows(String method) {
        Object result = call(method, new Class<?>[0]);
        return result instanceof List<?> list ? List.copyOf((List<String>) list) : List.of();
    }
    public static List<String> partyNames() { return rows("partyNames"); }
    public static List<String> friendNames() { return rows("friendNames"); }
    public static List<String> profileRows() { return rows("profileRows"); }
    public static boolean inParty() { return Boolean.TRUE.equals(call("inParty", new Class<?>[0])); }
    public static void open(String page) { call("open", new Class<?>[] {String.class}, page); }
    public static void invite(String player) { call("invite", new Class<?>[] {String.class}, player); }
    public static void leave() { call("leave", new Class<?>[0]); }
    public static boolean isBoss(Entity entity) {
        return Boolean.TRUE.equals(call("isBoss", new Class<?>[] {Entity.class}, entity));
    }
}
