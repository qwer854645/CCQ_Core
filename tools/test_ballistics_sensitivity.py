"""Exercise the compiled injection against isolated Minecraft API stubs (JDK 21)."""
from pathlib import Path
import os, shutil, subprocess, tempfile

root = Path(__file__).resolve().parents[1]
java = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-21")) / "bin"
sources = {
"net/minecraft/client/gui/screens/Screen.java": "package net.minecraft.client.gui.screens; public class Screen {}",
"net/minecraft/client/Minecraft.java": """package net.minecraft.client;
public class Minecraft {
 public static final Minecraft INSTANCE = new Minecraft();
 public net.minecraft.client.gui.screens.Screen screen;
 public final Options options = new Options();
 public static Minecraft getInstance() { return INSTANCE; }
}""",
"net/minecraft/client/Options.java": """package net.minecraft.client;
public class Options {
 public final OptionInstance<Double> sensitivity = new OptionInstance<>();
 public OptionInstance<Double> sensitivity() { return sensitivity; }
}""",
"net/minecraft/client/OptionInstance.java": """package net.minecraft.client;
public class OptionInstance<T> {
 public double value = .5;
 public int writes;
 @SuppressWarnings("unchecked") public T get() { return (T)Double.valueOf(value); }
 public void set(T v) { value = (Double)v; writes++; }
}""",
"net/neoforged/neoforge/client/event/ComputeFovModifierEvent.java": """package net.neoforged.neoforge.client.event;
public class ComputeFovModifierEvent {}""",
"org/spongepowered/asm/mixin/injection/callback/CallbackInfo.java": """package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfo { public boolean cancelled; public void cancel() { cancelled = true; } }""",
"SensitivityRegression.java": """
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
public class SensitivityRegression {
 static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
 public static void main(String[] args) throws Exception {
  Class<?> type = Class.forName("ccq.core.compat.ballistics.mixin.RangefinderSensitivityMixin");
  Object mixin = type.getConstructor().newInstance();
  var scoped = type.getDeclaredField("isScoped"); scoped.setAccessible(true);
  var saved = type.getDeclaredField("sensitivity"); saved.setAccessible(true);
  var method = type.getDeclaredMethod("ccq$preserveScreenSensitivity",
   net.neoforged.neoforge.client.event.ComputeFovModifierEvent.class, CallbackInfo.class);
  method.setAccessible(true);
  var mc = Minecraft.INSTANCE;
  var option = mc.options.sensitivity;
  saved.setDouble(mixin, .5);
  option.value = .73; mc.screen = new net.minecraft.client.gui.screens.Screen();
  var ci = new CallbackInfo(); method.invoke(mixin, null, ci);
  check(ci.cancelled && option.value == .73 && option.writes == 0, "GUI edit overwritten");
  check(saved.getDouble(mixin) == .73, "Baseline not refreshed");
  for (double v : new double[]{.1, .9, 0, 1, .42}) {
   option.value = v; method.invoke(mixin, null, new CallbackInfo());
   check(option.value == v && option.writes == 0, "Repeated GUI edit overwritten");
  }
  scoped.setBoolean(mixin, true); saved.setDouble(mixin, .8); option.value = .08;
  method.invoke(mixin, null, new CallbackInfo());
  check(option.value == .8 && !scoped.getBoolean(mixin) && option.writes == 1,
   "Scope exit must restore the baseline exactly once");
  option.value = .61; method.invoke(mixin, null, new CallbackInfo());
  check(option.value == .61 && option.writes == 1, "Scope cache overwrote a later edit");
  mc.screen = null; scoped.setBoolean(mixin, true); option.value = .061;
  ci = new CallbackInfo(); method.invoke(mixin, null, ci);
  check(!ci.cancelled && scoped.getBoolean(mixin) && option.value == .061,
   "Normal rangefinder handling must continue unchanged");
  System.out.println("PASS: GUI edits, repeated edits and endpoints, one-time scope restore, post-scope edits, normal scope passthrough");
 }
}"""
}
with tempfile.TemporaryDirectory(prefix="ccq-sensitivity-") as tmp:
 target = Path(tmp)
 for name, source in sources.items():
  f = target/name; f.parent.mkdir(parents=True, exist_ok=True); f.write_text(source)
 compiled = "ccq/core/compat/ballistics/mixin/RangefinderSensitivityMixin.class"
 f = target/compiled; f.parent.mkdir(parents=True, exist_ok=True)
 shutil.copy2(root/"build/classes/java/main"/compiled, f)
 subprocess.run([str(java/"javac.exe"), "-d", str(target), *map(str, target.rglob("*.java"))], check=True)
 subprocess.run([str(java/"java.exe"), "-cp", str(target), "SensitivityRegression"], check=True)
