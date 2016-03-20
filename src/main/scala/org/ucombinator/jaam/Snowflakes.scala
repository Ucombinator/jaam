package org.ucombinator.jaam

import scala.collection.JavaConversions._

import soot._
import soot.jimple._

// Snowflakes are special Java procedures whose behavior we know and special-case.
// For example, native methods (that would be difficult to analyze) are snowflakes.

case class SnowflakeBasePointer(val name : String) extends BasePointer

// Uniquely identifies a particular method somewhere in the program.
case class MethodDescription(val className : String,
                             val methodName : String,
                             val parameterTypes : List[String],
                             val returnType : String)

// Snowflakes are special-cased methods
abstract class SnowflakeHandler {
  def apply(state : State,
            nextStmt : Stmt,
            newFP : FramePointer,
            newStore : Store,
            newKontStack : KontStack) : Set[AbstractState]
}


object NoOpSnowflake extends SnowflakeHandler {
  override def apply(state : State,
                     nextStmt : Stmt,
                     newFP : FramePointer,
                     newStore : Store,
                     newKontStack : KontStack) : Set[AbstractState] =
    Set(state.copy(stmt = nextStmt))
}

// TODO/soundness: Add JohnSnowflake for black-holes. Not everything becomes top, but an awful lot will.

case class ReturnSnowflake(value : D) extends SnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, newFP : FramePointer, newStore : Store, newKontStack : KontStack) = {
    val newNewStore = state.stmt.sootStmt match {
      case sootStmt : DefinitionStmt => state.store.update(state.addrsOf(sootStmt.getLeftOp()), value)
      case sootStmt : InvokeStmt => state.store
    }
    val newState = state.copy(stmt = nextStmt)
    newState.setStore(newNewStore)
    Set(newState)
  }
}

case class PutStaticSnowflake(clas : String, field : String, v : soot.Value) extends SnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, newFP : FramePointer, newStore : Store, newKontStack : KontStack) = {
    val sootField = Jimple.v.newStaticFieldRef(Soot.getSootClass(clas).getFieldByName(field).makeRef())
    val tempState = state.copy(fp = newFP, kontStack = newKontStack)
    tempState.setStore(newStore)
    val value = tempState.eval(v)
    val newNewStore = state.store.update(state.addrsOf(sootField), value)
    val newState = state.copy(stmt = nextStmt)
    newState.setStore(newNewStore)
    Set(newState)
  }
}

object Snowflakes {
  val table = scala.collection.mutable.Map.empty[MethodDescription, SnowflakeHandler]
  def get(meth : SootMethod) : Option[SnowflakeHandler] =
    table.get(MethodDescription(
      meth.getDeclaringClass.getName,
      meth.getName,
      meth.getParameterTypes.toList.map(_.toString()),
      meth.getReturnType.toString()))

  // java.io.PrintStream
  table.put(MethodDescription("java.io.PrintStream", "println", List("int"), "void"), NoOpSnowflake)
  table.put(MethodDescription("java.io.PrintStream", "println", List("java.lang.String"), "void"), NoOpSnowflake)

  // java.lang.Class
  //private static native void registerNatives();
  //private static native java.lang.Class<?> forName0(java.lang.String, boolean, java.lang.ClassLoader, java.lang.Class<?>) throws java.lang.ClassNotFoundException;
  //public native boolean isInstance(java.lang.Object);
  //public native boolean isAssignableFrom(java.lang.Class<?>);
  //public native boolean isInterface();
  //public native boolean isArray();
  //public native boolean isPrimitive();
  //private native java.lang.String getName0();
  //public native java.lang.Class<? super T> getSuperclass();
  //public native java.lang.Class<?>[] getInterfaces();
  //public native java.lang.Class<?> getComponentType();
  //public native int getModifiers();
  //public native java.lang.Object[] getSigners();
  //native void setSigners(java.lang.Object[]);
  //private native java.lang.Object[] getEnclosingMethod0();
  //private native java.lang.Class<?> getDeclaringClass0();
  //private native java.security.ProtectionDomain getProtectionDomain0();
  //native void setProtectionDomain0(java.security.ProtectionDomain);
  //static native java.lang.Class getPrimitiveClass(java.lang.String);
  //private static native java.lang.reflect.Method getCheckMemberAccessMethod(java.lang.Class<? extends java.lang.SecurityManager>) throws java.lang.NoSuchMethodError;
  //private native java.lang.String getGenericSignature();
  //native byte[] getRawAnnotations();
  //native sun.reflect.ConstantPool getConstantPool();
  //private native java.lang.reflect.Field[] getDeclaredFields0(boolean);
  //private native java.lang.reflect.Method[] getDeclaredMethods0(boolean);
  //private native java.lang.reflect.Constructor<T>[] getDeclaredConstructors0(boolean);
  //private native java.lang.Class<?>[] getDeclaredClasses0();
  table.put(MethodDescription("java.lang.Class", "desiredAssertionStatus", List(), "boolean"), ReturnSnowflake(D.atomicTop))
  //private static native boolean desiredAssertionStatus0(java.lang.Class<?>);

  private def updateStore(oldStore : Store, clas : String, field : String, typ : String) =
    oldStore.update(StaticFieldAddr(Soot.getSootClass(clas).getFieldByName(field)),
      D(Set(ObjectValue(Soot.getSootClass(typ),
        SnowflakeBasePointer(clas + "." + field)))))

  table.put(MethodDescription("java.lang.Object", "clone", List(), "java.lang.Object"),
    new SnowflakeHandler {
      override def apply(state: State,
        nextStmt: Stmt,
        newFP: FramePointer,
        newStore: Store,
        newKontStack: KontStack): Set[AbstractState] = {
          val newNewStore = state.stmt.sootStmt match {
            case stmt : DefinitionStmt => {
              val value = stmt.getRightOp match {
                case expr: InstanceInvokeExpr => state.eval(expr.getBase)
              }
              state.store.update(state.addrsOf(stmt.getLeftOp), value)
            }
            case stmt : InvokeStmt => state.store
          }
          val newState = state.copy(stmt = nextStmt)
          newState.setStore(newNewStore)
          Set(newState)
      }
    })
  
  table.put(MethodDescription("java.security.AccessController", "getStackAccessControlContext",
    List(), "java.security.AccessControlContext"), NoOpSnowflake)
  table.put(MethodDescription("java.lang.Class", "registerNatives", List(), "void"), NoOpSnowflake)

  table.put(MethodDescription("java.lang.Double", "doubleToRawLongBits", List("double"), "long"), ReturnSnowflake(D.atomicTop))
  table.put(MethodDescription("java.lang.Float", "floatToRawIntBits", List("float"), "int"), ReturnSnowflake(D.atomicTop))

  table.put(MethodDescription("java.lang.Class", "getPrimitiveClass", List("java.lang.String"), "java.lang.Class"),
    new SnowflakeHandler {
      override def apply(state: State, nextStmt: Stmt, newFP: FramePointer, newStore: Store, newKontStack: KontStack): Set[AbstractState] = {
        val newNewStore = state.stmt.sootStmt match {
          case stmt : DefinitionStmt => state.store.update(state.addrsOf(stmt.getLeftOp),
            D(Set(ObjectValue(Soot.classes.Class, state.malloc()))))
          case stmt : InvokeStmt => state.store
        }
        val newState = state.copy(stmt = nextStmt)
        newState.setStore(newNewStore)
        Set(newState)
      }
    })

  table.put(MethodDescription("java.lang.System", "arraycopy",
    List("java.lang.Object", "int", "java.lang.Object", "int", "int"), "void"), new SnowflakeHandler {
      override def apply(state: State,
        nextStmt: Stmt,
        newFP: FramePointer,
        newStore: Store,
        newKontStack: KontStack): Set[AbstractState] = {
          assert(state.stmt.sootStmt.getInvokeExpr.getArgCount == 5)
          val expr = state.stmt.sootStmt.getInvokeExpr
          val newNewStore = newStore.update(state.addrsOf(expr.getArg(2)), state.eval(expr.getArg(0)))
          val newState = state.copy(stmt = nextStmt)
          newState.setStore(newNewStore)
          Set(newState)
      }
    })

  // java.lang.System
  table.put(MethodDescription("java.lang.System", SootMethod.staticInitializerName, List(), "void"),
    new SnowflakeHandler {
      override def apply(state : State, nextStmt : Stmt,
        newFP : FramePointer, newStore : Store, newKontStack : KontStack) = {
        var newNewStore = newStore
        newNewStore = updateStore(newNewStore, "java.lang.System", "in", "java.io.InputStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "out", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "err", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "security", "java.lang.SecurityManager")
        newNewStore = updateStore(newNewStore, "java.lang.System", "cons", "java.io.Console")
        val newState = state.copy(stmt = nextStmt)
        newState.setStore(newNewStore)
        Set(newState)
      }
    })
  //private static native void registerNatives();
  table.put(MethodDescription("java.lang.System", "setIn0", List("java.io.InputStream"), "void"),
    PutStaticSnowflake("java.lang.System", "in", new ParameterRef(Soot.getSootClass("java.io.InputStream").getType, 0)))
  table.put(MethodDescription("java.lang.System", "setOut0", List("java.io.PrintStream"), "void"),
    PutStaticSnowflake("java.lang.System", "out", new ParameterRef(Soot.getSootClass("java.io.PrintStream").getType, 0)))
  table.put(MethodDescription("java.lang.System", "setErr0", List("java.io.PrintStream"), "void"),
    PutStaticSnowflake("java.lang.System", "err", new ParameterRef(Soot.getSootClass("java.io.PrintStream").getType, 0)))
  table.put(MethodDescription("java.lang.System", "currentTimeMillis", List(), "long"), ReturnSnowflake(D.atomicTop))
  table.put(MethodDescription("java.lang.System", "nanoTime", List(), "long"), ReturnSnowflake(D.atomicTop))
  //public static native void arraycopy(java.lang.Object, int, java.lang.Object, int, int);
  table.put(MethodDescription("java.lang.System", "identityHashCode", List("java.lang.Object"), "int"), ReturnSnowflake(D.atomicTop))
  //private static native java.util.Properties initProperties(java.util.Properties);
  //public static native java.lang.String mapLibraryName(java.lang.String);

  // java.lang.Throwable
  table.put(MethodDescription("java.lang.Throwable", SootMethod.constructorName, List(), "void"), NoOpSnowflake)
  table.put(MethodDescription("java.lang.Throwable", SootMethod.staticInitializerName, List(), "void"), NoOpSnowflake)
  //private native java.lang.Throwable fillInStackTrace(int);
  table.put(MethodDescription("java.lang.Throwable", "getStackTraceDepth", List(), "int"), ReturnSnowflake(D.atomicTop))
  //native java.lang.StackTraceElement getStackTraceElement(int);

  // java.util.ArrayList
  //table.put(MethodDescription("java.util.ArrayList", SootMethod.constructorName, List("int"), "void"), NoOpSnowflake)
}

/*

****************************
* Native methods in rt.jar *
****************************

com/sun/demo/jvmti/hprof/Tracker.class: private static native void nativeCallSite(java.lang.Object, int, int);
com/sun/demo/jvmti/hprof/Tracker.class: private static native void nativeNewArray(java.lang.Object, java.lang.Object);
com/sun/demo/jvmti/hprof/Tracker.class: private static native void nativeObjectInit(java.lang.Object, java.lang.Object);
com/sun/demo/jvmti/hprof/Tracker.class: private static native void nativeReturnSite(java.lang.Object, int, int);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native boolean readImageHeader(long, boolean, boolean) throws java.io.IOException;
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native boolean readImage(long, byte[], int, int[], int[], int, int, int, int, int, int, javax.imageio.plugins.jpeg.JPEGQTable[], javax.imageio.plugins.jpeg.JPEGHuffmanTable[], javax.imageio.plugins.jpeg.JPEGHuffmanTable[], int, int, boolean);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native long initJPEGImageReader();
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native void abortRead(long);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native void resetLibraryState(long);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native void resetReader(long);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native void setOutColorSpace(long, int);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private native void setSource(long);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private static native void disposeReader(long);
com/sun/imageio/plugins/jpeg/JPEGImageReader.class: private static native void initReaderIDs(java.lang.Class, java.lang.Class, java.lang.Class);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native boolean writeImage(long, byte[], int, int, int, int[], int, int, int, int, int, javax.imageio.plugins.jpeg.JPEGQTable[], boolean, javax.imageio.plugins.jpeg.JPEGHuffmanTable[], javax.imageio.plugins.jpeg.JPEGHuffmanTable[], boolean, boolean, boolean, int, int[], int[], int[], int[], int[], boolean, int);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native long initJPEGImageWriter();
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native void abortWrite(long);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native void resetWriter(long);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native void setDest(long);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private native void writeTables(long, javax.imageio.plugins.jpeg.JPEGQTable[], javax.imageio.plugins.jpeg.JPEGHuffmanTable[], javax.imageio.plugins.jpeg.JPEGHuffmanTable[]);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private static native void disposeWriter(long);
com/sun/imageio/plugins/jpeg/JPEGImageWriter.class: private static native void initWriterIDs(java.lang.Class, java.lang.Class);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native int nativeFinishPainting(int[], int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native java.lang.Object native_get_gtk_setting(int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_arrow(int, int, int, java.lang.String, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_background(int, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_box_gap(int, int, int, java.lang.String, int, int, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_box(int, int, int, java.lang.String, int, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_check(int, int, java.lang.String, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_expander(int, int, java.lang.String, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_extension(int, int, int, java.lang.String, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_flat_box(int, int, int, java.lang.String, int, int, int, int, boolean);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_focus(int, int, java.lang.String, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_handle(int, int, int, java.lang.String, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_hline(int, int, java.lang.String, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_option(int, int, java.lang.String, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_shadow(int, int, int, java.lang.String, int, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_slider(int, int, int, java.lang.String, int, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_paint_vline(int, int, java.lang.String, int, int, int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void nativeSetRangeValue(int, double, double, double, double);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void nativeStartPainting(int, int);
com/sun/java/swing/plaf/gtk/GTKEngine.class: private native void native_switch_theme();
com/sun/java/swing/plaf/gtk/GTKStyle.class: private static native int nativeGetColorForState(int, int, int);
com/sun/java/swing/plaf/gtk/GTKStyle.class: private static native int nativeGetXThickness(int);
com/sun/java/swing/plaf/gtk/GTKStyle.class: private static native int nativeGetYThickness(int);
com/sun/java/swing/plaf/gtk/GTKStyle.class: private static native java.lang.Object nativeGetClassValue(int, java.lang.String);
com/sun/java/swing/plaf/gtk/GTKStyle.class: private static native java.lang.String nativeGetPangoFontName(int);
com/sun/java/util/jar/pack/NativeUnpack.class: private static synchronized native void initIDs();
com/sun/java/util/jar/pack/NativeUnpack.class: private synchronized native boolean getNextFile(java.lang.Object[]);
com/sun/java/util/jar/pack/NativeUnpack.class: private synchronized native java.nio.ByteBuffer getUnusedInput();
com/sun/java/util/jar/pack/NativeUnpack.class: private synchronized native long finish();
com/sun/java/util/jar/pack/NativeUnpack.class: private synchronized native long start(java.nio.ByteBuffer, long);
com/sun/java/util/jar/pack/NativeUnpack.class: protected synchronized native boolean setOption(java.lang.String, java.lang.String);
com/sun/java/util/jar/pack/NativeUnpack.class: protected synchronized native java.lang.String getOption(java.lang.String);
com/sun/management/UnixOperatingSystem.class: private static native void initialize();
com/sun/management/UnixOperatingSystem.class: public native double getProcessCpuLoad();
com/sun/management/UnixOperatingSystem.class: public native double getSystemCpuLoad();
com/sun/management/UnixOperatingSystem.class: public native long getCommittedVirtualMemorySize();
com/sun/management/UnixOperatingSystem.class: public native long getFreePhysicalMemorySize();
com/sun/management/UnixOperatingSystem.class: public native long getFreeSwapSpaceSize();
com/sun/management/UnixOperatingSystem.class: public native long getMaxFileDescriptorCount();
com/sun/management/UnixOperatingSystem.class: public native long getOpenFileDescriptorCount();
com/sun/management/UnixOperatingSystem.class: public native long getProcessCpuTime();
com/sun/management/UnixOperatingSystem.class: public native long getTotalPhysicalMemorySize();
com/sun/management/UnixOperatingSystem.class: public native long getTotalSwapSpaceSize();
com/sun/media/sound/DirectAudioDevice.class: private static native boolean nIsStillDraining(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native boolean nRequiresServicing(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native int nAvailable(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native int nGetBufferSize(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native int nRead(long, byte[], int, int, int);
com/sun/media/sound/DirectAudioDevice.class: private static native int nWrite(long, byte[], int, int, int, float, float);
com/sun/media/sound/DirectAudioDevice.class: private static native long nGetBytePosition(long, boolean, long);
com/sun/media/sound/DirectAudioDevice.class: private static native long nOpen(int, int, boolean, int, float, int, int, int, boolean, boolean, int) throws javax.sound.sampled.LineUnavailableException;
com/sun/media/sound/DirectAudioDevice.class: private static native void nClose(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native void nFlush(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native void nGetFormats(int, int, boolean, java.util.Vector);
com/sun/media/sound/DirectAudioDevice.class: private static native void nService(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native void nSetBytePosition(long, boolean, long);
com/sun/media/sound/DirectAudioDevice.class: private static native void nStart(long, boolean);
com/sun/media/sound/DirectAudioDevice.class: private static native void nStop(long, boolean);
com/sun/media/sound/DirectAudioDeviceProvider.class: private static native com.sun.media.sound.DirectAudioDeviceProvider$DirectAudioDeviceInfo nNewDirectAudioDeviceInfo(int);
com/sun/media/sound/DirectAudioDeviceProvider.class: private static native int nGetNumDevices();
com/sun/media/sound/MidiInDevice.class: private native long nGetTimeStamp(long);
com/sun/media/sound/MidiInDevice.class: private native long nOpen(int) throws javax.sound.midi.MidiUnavailableException;
com/sun/media/sound/MidiInDevice.class: private native void nClose(long);
com/sun/media/sound/MidiInDevice.class: private native void nGetMessages(long);
com/sun/media/sound/MidiInDevice.class: private native void nStart(long) throws javax.sound.midi.MidiUnavailableException;
com/sun/media/sound/MidiInDevice.class: private native void nStop(long);
com/sun/media/sound/MidiInDeviceProvider.class: private static native int nGetNumDevices();
com/sun/media/sound/MidiInDeviceProvider.class: private static native java.lang.String nGetDescription(int);
com/sun/media/sound/MidiInDeviceProvider.class: private static native java.lang.String nGetName(int);
com/sun/media/sound/MidiInDeviceProvider.class: private static native java.lang.String nGetVendor(int);
com/sun/media/sound/MidiInDeviceProvider.class: private static native java.lang.String nGetVersion(int);
com/sun/media/sound/MidiOutDevice.class: private native long nGetTimeStamp(long);
com/sun/media/sound/MidiOutDevice.class: private native long nOpen(int) throws javax.sound.midi.MidiUnavailableException;
com/sun/media/sound/MidiOutDevice.class: private native void nClose(long);
com/sun/media/sound/MidiOutDevice.class: private native void nSendLongMessage(long, byte[], int, long);
com/sun/media/sound/MidiOutDevice.class: private native void nSendShortMessage(long, int, long);
com/sun/media/sound/MidiOutDeviceProvider.class: private static native int nGetNumDevices();
com/sun/media/sound/MidiOutDeviceProvider.class: private static native java.lang.String nGetDescription(int);
com/sun/media/sound/MidiOutDeviceProvider.class: private static native java.lang.String nGetName(int);
com/sun/media/sound/MidiOutDeviceProvider.class: private static native java.lang.String nGetVendor(int);
com/sun/media/sound/MidiOutDeviceProvider.class: private static native java.lang.String nGetVersion(int);
com/sun/media/sound/Platform.class: private static native boolean nIsBigEndian();
com/sun/media/sound/Platform.class: private static native boolean nIsSigned8();
com/sun/media/sound/Platform.class: private static native int nGetLibraryForFeature(int);
com/sun/media/sound/Platform.class: private static native java.lang.String nGetExtraLibraries();
com/sun/media/sound/PortMixer.class: private static native float nControlGetFloatValue(long);
com/sun/media/sound/PortMixer.class: private static native int nControlGetIntValue(long);
com/sun/media/sound/PortMixer.class: private static native int nGetPortCount(long);
com/sun/media/sound/PortMixer.class: private static native int nGetPortType(long, int);
com/sun/media/sound/PortMixer.class: private static native java.lang.String nGetPortName(long, int);
com/sun/media/sound/PortMixer.class: private static native long nOpen(int) throws javax.sound.sampled.LineUnavailableException;
com/sun/media/sound/PortMixer.class: private static native void nClose(long);
com/sun/media/sound/PortMixer.class: private static native void nControlSetFloatValue(long, float);
com/sun/media/sound/PortMixer.class: private static native void nControlSetIntValue(long, int);
com/sun/media/sound/PortMixer.class: private static native void nGetControls(long, int, java.util.Vector);
com/sun/media/sound/PortMixerProvider.class: private static native com.sun.media.sound.PortMixerProvider$PortMixerInfo nNewPortMixerInfo(int);
com/sun/media/sound/PortMixerProvider.class: private static native int nGetNumDevices();
com/sun/security/auth/module/UnixSystem.class: private native void getUnixInfo();
java/awt/AWTEvent.class: private native void nativeSetSource(java.awt.peer.ComponentPeer);
java/awt/AWTEvent.class: private static native void initIDs();
java/awt/Button.class: private static native void initIDs();
java/awt/Checkbox.class: private static native void initIDs();
java/awt/CheckboxMenuItem.class: private static native void initIDs();
java/awt/Color.class: private static native void initIDs();
java/awt/Component.class: private static native void initIDs();
java/awt/Container.class: private static native void initIDs();
java/awt/Cursor.class: private static native void finalizeImpl(long);
java/awt/Cursor.class: private static native void initIDs();
java/awt/Dialog.class: private static native void initIDs();
java/awt/Dimension.class: private static native void initIDs();
java/awt/Event.class: private static native void initIDs();
java/awt/event/InputEvent.class: private static native void initIDs();
java/awt/event/KeyEvent.class: private static native void initIDs();
java/awt/event/MouseEvent.class: private static native void initIDs();
java/awt/FileDialog.class: private static native void initIDs();
java/awt/Font.class: private static native void initIDs();
java/awt/FontMetrics.class: private static native void initIDs();
java/awt/Frame.class: private static native void initIDs();
java/awt/image/BufferedImage.class: private static native void initIDs();
java/awt/image/ColorModel.class: private static native void initIDs();
java/awt/image/ComponentSampleModel.class: private static native void initIDs();
java/awt/image/IndexColorModel.class: private static native void initIDs();
java/awt/image/Kernel.class: private static native void initIDs();
java/awt/image/Raster.class: private static native void initIDs();
java/awt/image/SampleModel.class: private static native void initIDs();
java/awt/image/SinglePixelPackedSampleModel.class: private static native void initIDs();
java/awt/Insets.class: private static native void initIDs();
java/awt/KeyboardFocusManager.class: private static native void initIDs();
java/awt/Label.class: private static native void initIDs();
java/awt/MenuBar.class: private static native void initIDs();
java/awt/Menu.class: private static native void initIDs();
java/awt/MenuComponent.class: private static native void initIDs();
java/awt/MenuItem.class: private static native void initIDs();
java/awt/Rectangle.class: private static native void initIDs();
java/awt/Scrollbar.class: private static native void initIDs();
java/awt/ScrollPaneAdjustable.class: private static native void initIDs();
java/awt/ScrollPane.class: private static native void initIDs();
java/awt/SplashScreen.class: private static native boolean _isVisible(long);
java/awt/SplashScreen.class: private static native boolean _setImageData(long, byte[]);
java/awt/SplashScreen.class: private static native java.awt.Rectangle _getBounds(long);
java/awt/SplashScreen.class: private static native java.lang.String _getImageFileName(long);
java/awt/SplashScreen.class: private static native java.lang.String _getImageJarName(long);
java/awt/SplashScreen.class: private static native long _getInstance();
java/awt/SplashScreen.class: private static native void _close(long);
java/awt/SplashScreen.class: private static native void _update(long, int[], int, int, int, int, int);
java/awt/TextArea.class: private static native void initIDs();
java/awt/TextField.class: private static native void initIDs();
java/awt/Toolkit.class: private static native void initIDs();
java/awt/TrayIcon.class: private static native void initIDs();
java/awt/Window.class: private static native void initIDs();
java/io/Console.class: private static native boolean echo(boolean) throws java.io.IOException;
java/io/Console.class: private static native boolean istty();
java/io/Console.class: private static native java.lang.String encoding();
java/io/FileDescriptor.class: private static native void initIDs();
java/io/FileDescriptor.class: public native void sync() throws java.io.SyncFailedException;
java/io/FileInputStream.class: private native int read0() throws java.io.IOException;
java/io/FileInputStream.class: private native int readBytes(byte[], int, int) throws java.io.IOException;
java/io/FileInputStream.class: private native void close0() throws java.io.IOException;
java/io/FileInputStream.class: private native void open(java.lang.String) throws java.io.FileNotFoundException;
java/io/FileInputStream.class: private static native void initIDs();
java/io/FileInputStream.class: public native int available() throws java.io.IOException;
java/io/FileInputStream.class: public native long skip(long) throws java.io.IOException;
java/io/FileOutputStream.class: private native void close0() throws java.io.IOException;
java/io/FileOutputStream.class: private native void open(java.lang.String, boolean) throws java.io.FileNotFoundException;
java/io/FileOutputStream.class: private native void writeBytes(byte[], int, int, boolean) throws java.io.IOException;
java/io/FileOutputStream.class: private native void write(int, boolean) throws java.io.IOException;
java/io/FileOutputStream.class: private static native void initIDs();
java/io/FileSystem.class: public static native java.io.FileSystem getFileSystem();
java/io/ObjectInputStream.class: private static native void bytesToDoubles(byte[], int, double[], int, int);
java/io/ObjectInputStream.class: private static native void bytesToFloats(byte[], int, float[], int, int);
java/io/ObjectOutputStream.class: private static native void doublesToBytes(double[], int, byte[], int, int);
java/io/ObjectOutputStream.class: private static native void floatsToBytes(float[], int, byte[], int, int);
java/io/ObjectStreamClass.class: private static native boolean hasStaticInitializer(java.lang.Class<?>);
java/io/ObjectStreamClass.class: private static native void initNative();
java/io/RandomAccessFile.class: private native int read0() throws java.io.IOException;
java/io/RandomAccessFile.class: private native int readBytes0(byte[], int, int) throws java.io.IOException;
java/io/RandomAccessFile.class: private native void close0() throws java.io.IOException;
java/io/RandomAccessFile.class: private native void open(java.lang.String, int) throws java.io.FileNotFoundException;
java/io/RandomAccessFile.class: private native void write0(int) throws java.io.IOException;
java/io/RandomAccessFile.class: private native void writeBytes0(byte[], int, int) throws java.io.IOException;
java/io/RandomAccessFile.class: private static native void initIDs();
java/io/RandomAccessFile.class: public native long getFilePointer() throws java.io.IOException;
java/io/RandomAccessFile.class: public native long length() throws java.io.IOException;
java/io/RandomAccessFile.class: public native void seek(long) throws java.io.IOException;
java/io/RandomAccessFile.class: public native void setLength(long) throws java.io.IOException;
java/io/UnixFileSystem.class: private native boolean delete0(java.io.File);
java/io/UnixFileSystem.class: private native boolean rename0(java.io.File, java.io.File);
java/io/UnixFileSystem.class: private native java.lang.String canonicalize0(java.lang.String) throws java.io.IOException;
java/io/UnixFileSystem.class: private static native void initIDs();
java/io/UnixFileSystem.class: public native boolean checkAccess(java.io.File, int);
java/io/UnixFileSystem.class: public native boolean createDirectory(java.io.File);
java/io/UnixFileSystem.class: public native boolean createFileExclusively(java.lang.String) throws java.io.IOException;
java/io/UnixFileSystem.class: public native boolean setLastModifiedTime(java.io.File, long);
java/io/UnixFileSystem.class: public native boolean setPermission(java.io.File, int, boolean, boolean);
java/io/UnixFileSystem.class: public native boolean setReadOnly(java.io.File);
java/io/UnixFileSystem.class: public native int getBooleanAttributes0(java.io.File);
java/io/UnixFileSystem.class: public native java.lang.String[] list(java.io.File);
java/io/UnixFileSystem.class: public native long getLastModifiedTime(java.io.File);
java/io/UnixFileSystem.class: public native long getLength(java.io.File);
java/io/UnixFileSystem.class: public native long getSpace(java.io.File, int);
java/lang/Class.class: native byte[] getRawAnnotations();
java/lang/Class.class: native sun.reflect.ConstantPool getConstantPool();
java/lang/Class.class: native void setProtectionDomain0(java.security.ProtectionDomain);
java/lang/Class.class: native void setSigners(java.lang.Object[]);
java/lang/Class.class: private native java.lang.Class<?>[] getDeclaredClasses0();
java/lang/Class.class: private native java.lang.Class<?> getDeclaringClass0();
java/lang/Class.class: private native java.lang.Object[] getEnclosingMethod0();
java/lang/Class.class: private native java.lang.reflect.Constructor<T>[] getDeclaredConstructors0(boolean);
java/lang/Class.class: private native java.lang.reflect.Field[] getDeclaredFields0(boolean);
java/lang/Class.class: private native java.lang.reflect.Method[] getDeclaredMethods0(boolean);
java/lang/Class.class: private native java.lang.String getGenericSignature();
java/lang/Class.class: private native java.lang.String getName0();
java/lang/Class.class: private native java.security.ProtectionDomain getProtectionDomain0();
java/lang/Class.class: private static native boolean desiredAssertionStatus0(java.lang.Class<?>);
java/lang/Class.class: private static native java.lang.Class<?> forName0(java.lang.String, boolean, java.lang.ClassLoader, java.lang.Class<?>) throws java.lang.ClassNotFoundException;
java/lang/Class.class: private static native java.lang.reflect.Method getCheckMemberAccessMethod(java.lang.Class<? extends java.lang.SecurityManager>) throws java.lang.NoSuchMethodError;
java/lang/Class.class: private static native void registerNatives();
java/lang/Class.class: public native boolean isArray();
java/lang/Class.class: public native boolean isAssignableFrom(java.lang.Class<?>);
java/lang/Class.class: public native boolean isInstance(java.lang.Object);
java/lang/Class.class: public native boolean isInterface();
java/lang/Class.class: public native boolean isPrimitive();
java/lang/Class.class: public native int getModifiers();
java/lang/Class.class: public native java.lang.Class<?> getComponentType();
java/lang/Class.class: public native java.lang.Class<?>[] getInterfaces();
java/lang/Class.class: public native java.lang.Class<? super T> getSuperclass();
java/lang/Class.class: public native java.lang.Object[] getSigners();
java/lang/Class.class: static native java.lang.Class getPrimitiveClass(java.lang.String);
java/lang/ClassLoader.class: private final native java.lang.Class findLoadedClass0(java.lang.String);
java/lang/ClassLoader.class: private native java.lang.Class defineClass0(java.lang.String, byte[], int, int, java.security.ProtectionDomain);
java/lang/ClassLoader.class: private native java.lang.Class defineClass1(java.lang.String, byte[], int, int, java.security.ProtectionDomain, java.lang.String);
java/lang/ClassLoader.class: private native java.lang.Class defineClass2(java.lang.String, java.nio.ByteBuffer, int, int, java.security.ProtectionDomain, java.lang.String);
java/lang/ClassLoader.class: private native java.lang.Class findBootstrapClass(java.lang.String);
java/lang/ClassLoader.class: private native void resolveClass0(java.lang.Class);
java/lang/ClassLoader.class: private static native java.lang.AssertionStatusDirectives retrieveDirectives();
java/lang/ClassLoader.class: private static native void registerNatives();
java/lang/ClassLoader$NativeLibrary.class: native long find(java.lang.String);
java/lang/ClassLoader$NativeLibrary.class: native void load(java.lang.String);
java/lang/ClassLoader$NativeLibrary.class: native void unload();
java/lang/Compiler.class: private static native void initialize();
java/lang/Compiler.class: private static native void registerNatives();
java/lang/Compiler.class: public static native boolean compileClasses(java.lang.String);
java/lang/Compiler.class: public static native boolean compileClass(java.lang.Class<?>);
java/lang/Compiler.class: public static native java.lang.Object command(java.lang.Object);
java/lang/Compiler.class: public static native void disable();
java/lang/Compiler.class: public static native void enable();
java/lang/Double.class: public static native double longBitsToDouble(long);
java/lang/Double.class: public static native long doubleToRawLongBits(double);
java/lang/Float.class: public static native float intBitsToFloat(int);
java/lang/Float.class: public static native int floatToRawIntBits(float);
java/lang/invoke/MethodHandle.class: final native java.lang.Object invokeBasic(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: public final native java.lang.Object invokeExact(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: public final native java.lang.Object invoke(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: static native java.lang.Object linkToInterface(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: static native java.lang.Object linkToSpecial(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: static native java.lang.Object linkToStatic(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandle.class: static native java.lang.Object linkToVirtual(java.lang.Object...) throws java.lang.Throwable;
java/lang/invoke/MethodHandleNatives.class: private static native int getNamedCon(int, java.lang.Object[]);
java/lang/invoke/MethodHandleNatives.class: private static native void registerNatives();
java/lang/invoke/MethodHandleNatives.class: static native int getConstant(int);
java/lang/invoke/MethodHandleNatives.class: static native int getMembers(java.lang.Class<?>, java.lang.String, java.lang.String, int, java.lang.Class<?>, int, java.lang.invoke.MemberName[]);
java/lang/invoke/MethodHandleNatives.class: static native java.lang.invoke.MemberName resolve(java.lang.invoke.MemberName, java.lang.Class<?>) throws java.lang.LinkageError;
java/lang/invoke/MethodHandleNatives.class: static native java.lang.Object getMemberVMInfo(java.lang.invoke.MemberName);
java/lang/invoke/MethodHandleNatives.class: static native java.lang.Object staticFieldBase(java.lang.invoke.MemberName);
java/lang/invoke/MethodHandleNatives.class: static native long objectFieldOffset(java.lang.invoke.MemberName);
java/lang/invoke/MethodHandleNatives.class: static native long staticFieldOffset(java.lang.invoke.MemberName);
java/lang/invoke/MethodHandleNatives.class: static native void expand(java.lang.invoke.MemberName);
java/lang/invoke/MethodHandleNatives.class: static native void init(java.lang.invoke.MemberName, java.lang.Object);
java/lang/invoke/MethodHandleNatives.class: static native void setCallSiteTargetNormal(java.lang.invoke.CallSite, java.lang.invoke.MethodHandle);
java/lang/invoke/MethodHandleNatives.class: static native void setCallSiteTargetVolatile(java.lang.invoke.CallSite, java.lang.invoke.MethodHandle);
java/lang/Object.class: private static native void registerNatives();
java/lang/Object.class: protected native java.lang.Object clone() throws java.lang.CloneNotSupportedException;
java/lang/Object.class: public final native java.lang.Class<?> getClass();
java/lang/Object.class: public final native void notify();
java/lang/Object.class: public final native void notifyAll();
java/lang/Object.class: public final native void wait(long) throws java.lang.InterruptedException;
java/lang/Object.class: public native int hashCode();
java/lang/Package.class: private static native java.lang.String getSystemPackage0(java.lang.String);
java/lang/Package.class: private static native java.lang.String[] getSystemPackages0();
java/lang/ProcessEnvironment.class: private static native byte[][] environ();
java/lang/reflect/Array.class: private static native java.lang.Object multiNewArray(java.lang.Class, int[]) throws java.lang.IllegalArgumentException, java.lang.NegativeArraySizeException;
java/lang/reflect/Array.class: private static native java.lang.Object newArray(java.lang.Class, int) throws java.lang.NegativeArraySizeException;
java/lang/reflect/Array.class: public static native boolean getBoolean(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native byte getByte(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native char getChar(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native double getDouble(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native float getFloat(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native int getInt(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native int getLength(java.lang.Object) throws java.lang.IllegalArgumentException;
java/lang/reflect/Array.class: public static native java.lang.Object get(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native long getLong(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native short getShort(java.lang.Object, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setBoolean(java.lang.Object, int, boolean) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setByte(java.lang.Object, int, byte) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setChar(java.lang.Object, int, char) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setDouble(java.lang.Object, int, double) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setFloat(java.lang.Object, int, float) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setInt(java.lang.Object, int, int) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void set(java.lang.Object, int, java.lang.Object) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setLong(java.lang.Object, int, long) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Array.class: public static native void setShort(java.lang.Object, int, short) throws java.lang.IllegalArgumentException, java.lang.ArrayIndexOutOfBoundsException;
java/lang/reflect/Proxy.class: private static native java.lang.Class defineClass0(java.lang.ClassLoader, java.lang.String, byte[], int, int);
java/lang/Runtime.class: private static native void runFinalization0();
java/lang/Runtime.class: public native int availableProcessors();
java/lang/Runtime.class: public native long freeMemory();
java/lang/Runtime.class: public native long maxMemory();
java/lang/Runtime.class: public native long totalMemory();
java/lang/Runtime.class: public native void gc();
java/lang/Runtime.class: public native void traceInstructions(boolean);
java/lang/Runtime.class: public native void traceMethodCalls(boolean);
java/lang/SecurityManager.class: private native int classLoaderDepth0();
java/lang/SecurityManager.class: private native java.lang.Class currentLoadedClass0();
java/lang/SecurityManager.class: private native java.lang.ClassLoader currentClassLoader0();
java/lang/SecurityManager.class: protected native int classDepth(java.lang.String);
java/lang/SecurityManager.class: protected native java.lang.Class[] getClassContext();
java/lang/Shutdown.class: private static native void runAllFinalizers();
java/lang/Shutdown.class: static native void halt0(int);
java/lang/StrictMath.class: public static native double acos(double);
java/lang/StrictMath.class: public static native double asin(double);
java/lang/StrictMath.class: public static native double atan2(double, double);
java/lang/StrictMath.class: public static native double atan(double);
java/lang/StrictMath.class: public static native double cbrt(double);
java/lang/StrictMath.class: public static native double cos(double);
java/lang/StrictMath.class: public static native double cosh(double);
java/lang/StrictMath.class: public static native double exp(double);
java/lang/StrictMath.class: public static native double expm1(double);
java/lang/StrictMath.class: public static native double hypot(double, double);
java/lang/StrictMath.class: public static native double IEEEremainder(double, double);
java/lang/StrictMath.class: public static native double log10(double);
java/lang/StrictMath.class: public static native double log1p(double);
java/lang/StrictMath.class: public static native double log(double);
java/lang/StrictMath.class: public static native double pow(double, double);
java/lang/StrictMath.class: public static native double sin(double);
java/lang/StrictMath.class: public static native double sinh(double);
java/lang/StrictMath.class: public static native double sqrt(double);
java/lang/StrictMath.class: public static native double tan(double);
java/lang/StrictMath.class: public static native double tanh(double);
java/lang/String.class: public native java.lang.String intern();
java/lang/System.class: private static native java.util.Properties initProperties(java.util.Properties);
java/lang/System.class: private static native void registerNatives();
java/lang/System.class: private static native void setErr0(java.io.PrintStream);
java/lang/System.class: private static native void setIn0(java.io.InputStream);
java/lang/System.class: private static native void setOut0(java.io.PrintStream);
java/lang/System.class: public static native int identityHashCode(java.lang.Object);
java/lang/System.class: public static native java.lang.String mapLibraryName(java.lang.String);
java/lang/System.class: public static native long currentTimeMillis();
java/lang/System.class: public static native long nanoTime();
java/lang/System.class: public static native void arraycopy(java.lang.Object, int, java.lang.Object, int, int);
java/lang/Thread.class: private native boolean isInterrupted(boolean);
java/lang/Thread.class: private native void interrupt0();
java/lang/Thread.class: private native void resume0();
java/lang/Thread.class: private native void setNativeName(java.lang.String);
java/lang/Thread.class: private native void setPriority0(int);
java/lang/Thread.class: private native void start0();
java/lang/Thread.class: private native void stop0(java.lang.Object);
java/lang/Thread.class: private native void suspend0();
java/lang/Thread.class: private static native java.lang.StackTraceElement[][] dumpThreads(java.lang.Thread[]);
java/lang/Thread.class: private static native java.lang.Thread[] getThreads();
java/lang/Thread.class: private static native void registerNatives();
java/lang/Thread.class: public final native boolean isAlive();
java/lang/Thread.class: public native int countStackFrames();
java/lang/Thread.class: public static native boolean holdsLock(java.lang.Object);
java/lang/Thread.class: public static native java.lang.Thread currentThread();
java/lang/Thread.class: public static native void sleep(long) throws java.lang.InterruptedException;
java/lang/Thread.class: public static native void yield();
java/lang/Throwable.class: native int getStackTraceDepth();
java/lang/Throwable.class: native java.lang.StackTraceElement getStackTraceElement(int);
java/lang/Throwable.class: private native java.lang.Throwable fillInStackTrace(int);
java/lang/UNIXProcess.class: private native int forkAndExec(int, byte[], byte[], byte[], int, byte[], int, byte[], int[], boolean) throws java.io.IOException;
java/lang/UNIXProcess.class: private native int waitForProcessExit(int);
java/lang/UNIXProcess.class: private static native void destroyProcess(int);
java/lang/UNIXProcess.class: private static native void init();
java/net/AbstractPlainDatagramSocketImpl.class: native int dataAvailable();
java/net/AbstractPlainDatagramSocketImpl.class: private static native void init();
java/net/DatagramPacket.class: private static native void init();
java/net/Inet4Address.class: private static native void init();
java/net/Inet4AddressImpl.class: private native boolean isReachable0(byte[], int, byte[], int) throws java.io.IOException;
java/net/Inet4AddressImpl.class: public native java.lang.String getHostByAddr(byte[]) throws java.net.UnknownHostException;
java/net/Inet4AddressImpl.class: public native java.lang.String getLocalHostName() throws java.net.UnknownHostException;
java/net/Inet4AddressImpl.class: public native java.net.InetAddress[] lookupAllHostAddr(java.lang.String) throws java.net.UnknownHostException;
java/net/Inet6Address.class: private static native void init();
java/net/Inet6AddressImpl.class: private native boolean isReachable0(byte[], int, int, byte[], int, int) throws java.io.IOException;
java/net/Inet6AddressImpl.class: public native java.lang.String getHostByAddr(byte[]) throws java.net.UnknownHostException;
java/net/Inet6AddressImpl.class: public native java.lang.String getLocalHostName() throws java.net.UnknownHostException;
java/net/Inet6AddressImpl.class: public native java.net.InetAddress[] lookupAllHostAddr(java.lang.String) throws java.net.UnknownHostException;
java/net/InetAddress.class: private static native void init();
java/net/InetAddressImplFactory.class: static native boolean isIPv6Supported();
java/net/NetworkInterface.class: private static native boolean isLoopback0(java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native boolean isP2P0(java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native boolean isUp0(java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native boolean supportsMulticast0(java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native byte[] getMacAddr0(byte[], java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native int getMTU0(java.lang.String, int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native java.net.NetworkInterface[] getAll() throws java.net.SocketException;
java/net/NetworkInterface.class: private static native java.net.NetworkInterface getByIndex0(int) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native java.net.NetworkInterface getByInetAddress0(java.net.InetAddress) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native java.net.NetworkInterface getByName0(java.lang.String) throws java.net.SocketException;
java/net/NetworkInterface.class: private static native void init();
java/net/PlainDatagramSocketImpl.class: private static native void init();
java/net/PlainDatagramSocketImpl.class: protected native byte getTTL() throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native int getTimeToLive() throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native java.lang.Object socketGetOption(int) throws java.net.SocketException;
java/net/PlainDatagramSocketImpl.class: protected native void connect0(java.net.InetAddress, int) throws java.net.SocketException;
java/net/PlainDatagramSocketImpl.class: protected native void datagramSocketClose();
java/net/PlainDatagramSocketImpl.class: protected native void datagramSocketCreate() throws java.net.SocketException;
java/net/PlainDatagramSocketImpl.class: protected native void disconnect0(int);
java/net/PlainDatagramSocketImpl.class: protected native void join(java.net.InetAddress, java.net.NetworkInterface) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native void leave(java.net.InetAddress, java.net.NetworkInterface) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native void send(java.net.DatagramPacket) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native void setTimeToLive(int) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native void setTTL(byte) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected native void socketSetOption(int, java.lang.Object) throws java.net.SocketException;
java/net/PlainDatagramSocketImpl.class: protected synchronized native int peekData(java.net.DatagramPacket) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected synchronized native int peek(java.net.InetAddress) throws java.io.IOException;
java/net/PlainDatagramSocketImpl.class: protected synchronized native void bind0(int, java.net.InetAddress) throws java.net.SocketException;
java/net/PlainDatagramSocketImpl.class: protected synchronized native void receive0(java.net.DatagramPacket) throws java.io.IOException;
java/net/PlainSocketImpl.class: native int socketAvailable() throws java.io.IOException;
java/net/PlainSocketImpl.class: native int socketGetOption(int, java.lang.Object) throws java.net.SocketException;
java/net/PlainSocketImpl.class: native void socketAccept(java.net.SocketImpl) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketBind(java.net.InetAddress, int) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketClose0(boolean) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketConnect(java.net.InetAddress, int, int) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketCreate(boolean) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketListen(int) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketSendUrgentData(int) throws java.io.IOException;
java/net/PlainSocketImpl.class: native void socketSetOption(int, boolean, java.lang.Object) throws java.net.SocketException;
java/net/PlainSocketImpl.class: native void socketShutdown(int) throws java.io.IOException;
java/net/PlainSocketImpl.class: static native void initProto();
java/net/SocketInputStream.class: private native int socketRead0(java.io.FileDescriptor, byte[], int, int, int) throws java.io.IOException;
java/net/SocketInputStream.class: private static native void init();
java/net/SocketOutputStream.class: private native void socketWrite0(java.io.FileDescriptor, byte[], int, int) throws java.io.IOException;
java/net/SocketOutputStream.class: private static native void init();
java/nio/Bits.class: static native void copyFromIntArray(java.lang.Object, long, long, long);
java/nio/Bits.class: static native void copyFromLongArray(java.lang.Object, long, long, long);
java/nio/Bits.class: static native void copyFromShortArray(java.lang.Object, long, long, long);
java/nio/Bits.class: static native void copyToIntArray(long, java.lang.Object, long, long);
java/nio/Bits.class: static native void copyToLongArray(long, java.lang.Object, long, long);
java/nio/Bits.class: static native void copyToShortArray(long, java.lang.Object, long, long);
java/nio/MappedByteBuffer.class: private native boolean isLoaded0(long, long, int);
java/nio/MappedByteBuffer.class: private native void force0(java.io.FileDescriptor, long, long);
java/nio/MappedByteBuffer.class: private native void load0(long, long);
java/security/AccessController.class: private static native java.security.AccessControlContext getStackAccessControlContext();
java/security/AccessController.class: public static native <T> T doPrivileged(java.security.PrivilegedAction<T>);
java/security/AccessController.class: public static native <T> T doPrivileged(java.security.PrivilegedAction<T>, java.security.AccessControlContext);
java/security/AccessController.class: public static native <T> T doPrivileged(java.security.PrivilegedExceptionAction<T>, java.security.AccessControlContext) throws java.security.PrivilegedActionException;
java/security/AccessController.class: public static native <T> T doPrivileged(java.security.PrivilegedExceptionAction<T>) throws java.security.PrivilegedActionException;
java/security/AccessController.class: static native java.security.AccessControlContext getInheritedAccessControlContext();
java/util/concurrent/atomic/AtomicLong.class: private static native boolean VMSupportsCS8();
java/util/jar/JarFile.class: private native java.lang.String[] getMetaInfEntryNames();
java/util/logging/FileHandler.class: private static native boolean isSetUID();
java/util/prefs/FileSystemPreferences.class: private static native int chmod(java.lang.String, int);
java/util/prefs/FileSystemPreferences.class: private static native int[] lockFile0(java.lang.String, int, boolean);
java/util/prefs/FileSystemPreferences.class: private static native int unlockFile0(int);
java/util/TimeZone.class: private static native java.lang.String getSystemGMTOffsetID();
java/util/TimeZone.class: private static native java.lang.String getSystemTimeZoneID(java.lang.String, java.lang.String);
java/util/zip/Adler32.class: private static native int updateByteBuffer(int, long, int, int);
java/util/zip/Adler32.class: private static native int updateBytes(int, byte[], int, int);
java/util/zip/Adler32.class: private static native int update(int, int);
java/util/zip/CRC32.class: private static native int updateBytes(int, byte[], int, int);
java/util/zip/CRC32.class: private static native int update(int, int);
java/util/zip/Deflater.class: private native int deflateBytes(long, byte[], int, int, int);
java/util/zip/Deflater.class: private static native int getAdler(long);
java/util/zip/Deflater.class: private static native long init(int, int, boolean);
java/util/zip/Deflater.class: private static native void end(long);
java/util/zip/Deflater.class: private static native void initIDs();
java/util/zip/Deflater.class: private static native void reset(long);
java/util/zip/Deflater.class: private static native void setDictionary(long, byte[], int, int);
java/util/zip/Inflater.class: private native int inflateBytes(long, byte[], int, int) throws java.util.zip.DataFormatException;
java/util/zip/Inflater.class: private static native int getAdler(long);
java/util/zip/Inflater.class: private static native long init(boolean);
java/util/zip/Inflater.class: private static native void end(long);
java/util/zip/Inflater.class: private static native void initIDs();
java/util/zip/Inflater.class: private static native void reset(long);
java/util/zip/Inflater.class: private static native void setDictionary(long, byte[], int, int);
java/util/zip/ZipFile.class: private static native boolean startsWithLOC(long);
java/util/zip/ZipFile.class: private static native byte[] getCommentBytes(long);
java/util/zip/ZipFile.class: private static native byte[] getEntryBytes(long, int);
java/util/zip/ZipFile.class: private static native int getEntryFlag(long);
java/util/zip/ZipFile.class: private static native int getEntryMethod(long);
java/util/zip/ZipFile.class: private static native int getTotal(long);
java/util/zip/ZipFile.class: private static native int read(long, long, long, byte[], int, int);
java/util/zip/ZipFile.class: private static native java.lang.String getZipMessage(long);
java/util/zip/ZipFile.class: private static native long getEntryCrc(long);
java/util/zip/ZipFile.class: private static native long getEntryCSize(long);
java/util/zip/ZipFile.class: private static native long getEntry(long, byte[], boolean);
java/util/zip/ZipFile.class: private static native long getEntrySize(long);
java/util/zip/ZipFile.class: private static native long getEntryTime(long);
java/util/zip/ZipFile.class: private static native long getNextEntry(long, int);
java/util/zip/ZipFile.class: private static native long open(java.lang.String, int, long, boolean) throws java.io.IOException;
java/util/zip/ZipFile.class: private static native void close(long);
java/util/zip/ZipFile.class: private static native void freeEntry(long, long);
java/util/zip/ZipFile.class: private static native void initIDs();
sun/awt/DebugSettings.class: private synchronized native void setCTracingOn(boolean);
sun/awt/DebugSettings.class: private synchronized native void setCTracingOn(boolean, java.lang.String);
sun/awt/DebugSettings.class: private synchronized native void setCTracingOn(boolean, java.lang.String, int);
sun/awt/DefaultMouseInfoPeer.class: public native boolean isWindowUnderMouse(java.awt.Window);
sun/awt/DefaultMouseInfoPeer.class: public native int fillPointWithCoords(java.awt.Point);
sun/awt/FontDescriptor.class: private static native void initIDs();
sun/awt/image/BufImgSurfaceData.class: private static native void freeNativeICMData(long);
sun/awt/image/BufImgSurfaceData.class: private static native void initIDs(java.lang.Class, java.lang.Class);
sun/awt/image/BufImgSurfaceData.class: protected native void initRaster(java.lang.Object, int, int, int, int, int, int, java.awt.image.IndexColorModel);
sun/awt/image/ByteComponentRaster.class: private static native void initIDs();
sun/awt/image/BytePackedRaster.class: private static native void initIDs();
sun/awt/image/DataBufferNative.class: protected native int getElem(int, int, sun.java2d.SurfaceData);
sun/awt/image/DataBufferNative.class: protected native void setElem(int, int, int, sun.java2d.SurfaceData);
sun/awt/image/GifImageDecoder.class: private native boolean parseImage(int, int, int, int, boolean, int, byte[], byte[], java.awt.image.IndexColorModel);
sun/awt/image/GifImageDecoder.class: private static native void initIDs();
sun/awt/image/ImageRepresentation.class: private native boolean setDiffICM(int, int, int, int, int[], int, int, java.awt.image.IndexColorModel, byte[], int, int, sun.awt.image.ByteComponentRaster, int);
sun/awt/image/ImageRepresentation.class: private native boolean setICMpixels(int, int, int, int, int[], byte[], int, int, sun.awt.image.IntegerComponentRaster);
sun/awt/image/ImageRepresentation.class: private static native void initIDs();
sun/awt/image/ImagingLib.class: private static native boolean init();
sun/awt/image/ImagingLib.class: public static native int convolveBI(java.awt.image.BufferedImage, java.awt.image.BufferedImage, java.awt.image.Kernel, int);
sun/awt/image/ImagingLib.class: public static native int convolveRaster(java.awt.image.Raster, java.awt.image.Raster, java.awt.image.Kernel, int);
sun/awt/image/ImagingLib.class: public static native int lookupByteBI(java.awt.image.BufferedImage, java.awt.image.BufferedImage, byte[][]);
sun/awt/image/ImagingLib.class: public static native int lookupByteRaster(java.awt.image.Raster, java.awt.image.Raster, byte[][]);
sun/awt/image/ImagingLib.class: public static native int transformBI(java.awt.image.BufferedImage, java.awt.image.BufferedImage, double[], int);
sun/awt/image/ImagingLib.class: public static native int transformRaster(java.awt.image.Raster, java.awt.image.Raster, double[], int);
sun/awt/image/IntegerComponentRaster.class: private static native void initIDs();
sun/awt/image/JPEGImageDecoder.class: private native void readImage(java.io.InputStream, byte[]) throws sun.awt.image.ImageFormatException, java.io.IOException;
sun/awt/image/JPEGImageDecoder.class: private static native void initIDs(java.lang.Class);
sun/awt/image/ShortComponentRaster.class: private static native void initIDs();
sun/awt/motif/AWTLockAccess.class: static native void awtLock();
sun/awt/motif/AWTLockAccess.class: static native void awtNotifyAll();
sun/awt/motif/AWTLockAccess.class: static native void awtUnlock();
sun/awt/motif/AWTLockAccess.class: static native void awtWait(long);
sun/awt/motif/MFontPeer.class: private static native void initIDs();
sun/awt/motif/MToolkit.class: private native boolean isSyncFailed();
sun/awt/motif/MToolkit.class: private native boolean isSyncUpdated();
sun/awt/motif/MToolkit.class: private native int getEventNumber();
sun/awt/motif/MToolkit.class: private native int getMulticlickTime();
sun/awt/motif/MToolkit.class: private native void loadXSettings();
sun/awt/motif/MToolkit.class: private native void nativeGrab(java.awt.peer.WindowPeer);
sun/awt/motif/MToolkit.class: private native void nativeUnGrab(java.awt.peer.WindowPeer);
sun/awt/motif/MToolkit.class: private native void shutdown();
sun/awt/motif/MToolkit.class: private native void updateSyncSelection();
sun/awt/motif/MToolkit.class: private static native java.lang.String getWMName();
sun/awt/motif/MToolkit.class: protected native boolean isDynamicLayoutSupportedNative();
sun/awt/motif/MToolkit.class: protected native int getScreenHeight();
sun/awt/motif/MToolkit.class: protected native int getScreenWidth();
sun/awt/motif/MToolkit.class: public native boolean getLockingKeyStateNative(int);
sun/awt/motif/MToolkit.class: public native boolean isFrameStateSupported(int);
sun/awt/motif/MToolkit.class: public native int getScreenResolution();
sun/awt/motif/MToolkit.class: public native void beep();
sun/awt/motif/MToolkit.class: public native void init(java.lang.String);
sun/awt/motif/MToolkit.class: public native void loadSystemColors(int[]);
sun/awt/motif/MToolkit.class: public native void run();
sun/awt/motif/MToolkit.class: static native java.awt.image.ColorModel makeColorModel();
sun/awt/motif/MToolkitThreadBlockedHandler.class: public native void enter();
sun/awt/motif/MToolkitThreadBlockedHandler.class: public native void exit();
sun/awt/motif/MWindowAttributes.class: private static native void initIDs();
sun/awt/motif/X11FontMetrics.class: native void init();
sun/awt/motif/X11FontMetrics.class: private native int getMFCharsWidth(char[], int, int, java.awt.Font);
sun/awt/motif/X11FontMetrics.class: private static native void initIDs();
sun/awt/motif/X11FontMetrics.class: public native int bytesWidth(byte[], int, int);
sun/awt/PlatformFont.class: private static native void initIDs();
sun/awt/SunToolkit.class: public static native void closeSplashScreen();
sun/awt/UNIXToolkit.class: private native boolean gtkCheckVersionImpl(int, int, int);
sun/awt/UNIXToolkit.class: private native boolean load_gtk_icon(java.lang.String);
sun/awt/UNIXToolkit.class: private native boolean load_stock_icon(int, java.lang.String, int, int, java.lang.String);
sun/awt/UNIXToolkit.class: private native void nativeSync();
sun/awt/UNIXToolkit.class: private static native boolean check_gtk();
sun/awt/UNIXToolkit.class: private static native boolean load_gtk();
sun/awt/UNIXToolkit.class: private static native boolean unload_gtk();
sun/awt/X11FontManager.class: public synchronized native java.lang.String getFontPathNative(boolean);
sun/awt/X11GraphicsConfig.class: private native boolean isTranslucencyCapable(long);
sun/awt/X11GraphicsConfig.class: private native double getXResolution(int);
sun/awt/X11GraphicsConfig.class: private native double getYResolution(int);
sun/awt/X11GraphicsConfig.class: private native int getNumColors();
sun/awt/X11GraphicsConfig.class: private native java.awt.image.ColorModel makeColorModel();
sun/awt/X11GraphicsConfig.class: private native long createBackBuffer(long, int);
sun/awt/X11GraphicsConfig.class: private native void init(int, int);
sun/awt/X11GraphicsConfig.class: private native void swapBuffers(long, int);
sun/awt/X11GraphicsConfig.class: private static native void dispose(long);
sun/awt/X11GraphicsConfig.class: private static native void initIDs();
sun/awt/X11GraphicsConfig.class: public native java.awt.Rectangle pGetBounds(int);
sun/awt/X11GraphicsConfig.class: public native void destroyBackBuffer(long);
sun/awt/X11GraphicsDevice.class: private native void getDoubleBufferVisuals(int);
sun/awt/X11GraphicsDevice.class: private static native boolean initXrandrExtension();
sun/awt/X11GraphicsDevice.class: private static native java.awt.DisplayMode getCurrentDisplayMode(int);
sun/awt/X11GraphicsDevice.class: private static native void configDisplayMode(int, int, int, int);
sun/awt/X11GraphicsDevice.class: private static native void enterFullScreenExclusive(long);
sun/awt/X11GraphicsDevice.class: private static native void enumDisplayModes(int, java.util.ArrayList<java.awt.DisplayMode>);
sun/awt/X11GraphicsDevice.class: private static native void exitFullScreenExclusive(long);
sun/awt/X11GraphicsDevice.class: private static native void initIDs();
sun/awt/X11GraphicsDevice.class: private static native void resetNativeData(int);
sun/awt/X11GraphicsDevice.class: public native int getConfigColormap(int, int);
sun/awt/X11GraphicsDevice.class: public native int getConfigDepth(int, int);
sun/awt/X11GraphicsDevice.class: public native int getConfigVisualId(int, int);
sun/awt/X11GraphicsDevice.class: public native int getNumConfigs(int);
sun/awt/X11GraphicsDevice.class: public native long getDisplay();
sun/awt/X11GraphicsDevice.class: public static native boolean isDBESupported();
sun/awt/X11GraphicsEnvironment.class: private static native boolean initGLX();
sun/awt/X11GraphicsEnvironment.class: private static native boolean initXRender(boolean);
sun/awt/X11GraphicsEnvironment.class: private static native boolean pRunningXinerama();
sun/awt/X11GraphicsEnvironment.class: private static native int checkShmExt();
sun/awt/X11GraphicsEnvironment.class: private static native java.awt.Point getXineramaCenterPoint();
sun/awt/X11GraphicsEnvironment.class: private static native java.lang.String getDisplayString();
sun/awt/X11GraphicsEnvironment.class: private static native void initDisplay(boolean);
sun/awt/X11GraphicsEnvironment.class: protected native int getDefaultScreenNum();
sun/awt/X11GraphicsEnvironment.class: protected native int getNumScreens();
sun/awt/X11/GtkFileDialogPeer.class: private native void quit();
sun/awt/X11/GtkFileDialogPeer.class: private native void run(java.lang.String, int, java.lang.String, java.lang.String, java.io.FilenameFilter, boolean, int, int);
sun/awt/X11/GtkFileDialogPeer.class: private static native void initIDs();
sun/awt/X11/GtkFileDialogPeer.class: public native void setBounds(int, int, int, int, int);
sun/awt/X11/GtkFileDialogPeer.class: public native void toFront();
sun/awt/X11InputMethod.class: private native boolean isCompositionEnabledNative();
sun/awt/X11InputMethod.class: private native boolean setCompositionEnabledNative(boolean);
sun/awt/X11InputMethod.class: private native void disposeXIC();
sun/awt/X11InputMethod.class: private native void turnoffStatusWindow();
sun/awt/X11InputMethod.class: private static native void initIDs();
sun/awt/X11InputMethod.class: protected native java.lang.String resetXIC();
sun/awt/X11/XDesktopPeer.class: private native boolean gnome_url_show(byte[]);
sun/awt/X11/XDesktopPeer.class: private static native boolean init();
sun/awt/X11/XFontPeer.class: private static native void initIDs();
sun/awt/X11/XInputMethod.class: private native boolean createXICNative(long);
sun/awt/X11/XInputMethod.class: private native boolean openXIMNative(long);
sun/awt/X11/XInputMethod.class: private native void adjustStatusWindow(long);
sun/awt/X11/XInputMethod.class: private native void setXICFocusNative(long, boolean, boolean);
sun/awt/X11/XlibWrapper.class: public static native int XSynchronize(long, boolean);
sun/awt/X11/XlibWrapper.class: static native boolean IsKanaKeyboard(long);
sun/awt/X11/XlibWrapper.class: static native boolean IsKeypadKey(long);
sun/awt/X11/XlibWrapper.class: static native boolean IsSunKeyboard(long);
sun/awt/X11/XlibWrapper.class: static native boolean IsXsunKPBehavior(long);
sun/awt/X11/XlibWrapper.class: static native boolean XAllocColor(long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XFilterEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XkbLibraryVersion(long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XkbQueryExtension(long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XkbTranslateKeyCode(long, int, long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XNextSecondaryLoopEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XQueryBestCursor(long, long, int, int, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XQueryExtension(long, java.lang.String, long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XQueryPointer(long, long, long, long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XShapeQueryExtension(long, long, long);
sun/awt/X11/XlibWrapper.class: static native boolean XSupportsLocale();
sun/awt/X11/XlibWrapper.class: static native byte[] getStringBytes(long);
sun/awt/X11/XlibWrapper.class: static native int CallErrorHandler(long, long, long);
sun/awt/X11/XlibWrapper.class: static native int DoesBackingStore(long);
sun/awt/X11/XlibWrapper.class: static native int ScreenCount(long);
sun/awt/X11/XlibWrapper.class: static native int VendorRelease(long);
sun/awt/X11/XlibWrapper.class: static native int XCreateFontCursor(long, int);
sun/awt/X11/XlibWrapper.class: static native int XdbeBeginIdiom(long);
sun/awt/X11/XlibWrapper.class: static native int XdbeDeallocateBackBufferName(long, long);
sun/awt/X11/XlibWrapper.class: static native int XdbeEndIdiom(long);
sun/awt/X11/XlibWrapper.class: static native int XdbeQueryExtension(long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XdbeSwapBuffers(long, long, int);
sun/awt/X11/XlibWrapper.class: static native int XEventsQueued(long, int);
sun/awt/X11/XlibWrapper.class: static native int XGetGeometry(long, long, long, long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XGetIconSizes(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XGetPointerMapping(long, long, int);
sun/awt/X11/XlibWrapper.class: static native int XGetWindowAttributes(long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XGetWindowProperty(long, long, long, long, long, long, long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XGetWMNormalHints(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XGrabKeyboard(long, long, int, int, int, long);
sun/awt/X11/XlibWrapper.class: static native int XGrabPointer(long, long, int, int, int, int, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XIconifyWindow(long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XInternAtoms(long, java.lang.String[], boolean, long);
sun/awt/X11/XlibWrapper.class: static native int XkbGetEffectiveGroup(long);
sun/awt/X11/XlibWrapper.class: static native int XKeysymToKeycode(long, long);
sun/awt/X11/XlibWrapper.class: static native int XQueryTree(long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native int XSendEvent(long, long, boolean, long, long);
sun/awt/X11/XlibWrapper.class: static native int XTranslateCoordinates(long, long, long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native java.lang.String GetProperty(long, long, long);
sun/awt/X11/XlibWrapper.class: static native java.lang.String ServerVendor(long);
sun/awt/X11/XlibWrapper.class: static native java.lang.String XGetAtomName(long, long);
sun/awt/X11/XlibWrapper.class: static native java.lang.String XGetDefault(long, java.lang.String, java.lang.String);
sun/awt/X11/XlibWrapper.class: static native java.lang.String XSetLocaleModifiers(java.lang.String);
sun/awt/X11/XlibWrapper.class: static native java.lang.String[] XTextPropertyToStringList(byte[], long);
sun/awt/X11/XlibWrapper.class: static native long DefaultScreen(long);
sun/awt/X11/XlibWrapper.class: static native long DisplayHeight(long, long);
sun/awt/X11/XlibWrapper.class: static native long DisplayHeightMM(long, long);
sun/awt/X11/XlibWrapper.class: static native long DisplayWidth(long, long);
sun/awt/X11/XlibWrapper.class: static native long DisplayWidthMM(long, long);
sun/awt/X11/XlibWrapper.class: static native long getAddress(java.lang.Object);
sun/awt/X11/XlibWrapper.class: static native long getScreenOfWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native long InternAtom(long, java.lang.String, int);
sun/awt/X11/XlibWrapper.class: static native long RootWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native long ScreenOfDisplay(long, long);
sun/awt/X11/XlibWrapper.class: static native long SetToolkitErrorHandler();
sun/awt/X11/XlibWrapper.class: static native long XAllocSizeHints();
sun/awt/X11/XlibWrapper.class: static native long XAllocWMHints();
sun/awt/X11/XlibWrapper.class: static native long XCreateBitmapFromData(long, long, long, int, int);
sun/awt/X11/XlibWrapper.class: static native long XCreateGC(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native long XCreateImage(long, long, int, int, int, long, int, int, int, int);
sun/awt/X11/XlibWrapper.class: static native long XCreatePixmapCursor(long, long, long, long, long, int, int);
sun/awt/X11/XlibWrapper.class: static native long XCreatePixmap(long, long, int, int, int);
sun/awt/X11/XlibWrapper.class: static native long XCreateWindow(long, long, int, int, int, int, int, int, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native long XdbeAllocateBackBufferName(long, long, int);
sun/awt/X11/XlibWrapper.class: static native long XDisplayString(long);
sun/awt/X11/XlibWrapper.class: static native long XGetInputFocus(long);
sun/awt/X11/XlibWrapper.class: static native long XGetModifierMapping(long);
sun/awt/X11/XlibWrapper.class: static native long XGetSelectionOwner(long, long);
sun/awt/X11/XlibWrapper.class: static native long XGetVisualInfo(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native long XkbGetMap(long, long, long);
sun/awt/X11/XlibWrapper.class: static native long XkbGetUpdatedMap(long, long, long);
sun/awt/X11/XlibWrapper.class: static native long XkbKeycodeToKeysym(long, int, int, int);
sun/awt/X11/XlibWrapper.class: static native long XKeycodeToKeysym(long, int, int);
sun/awt/X11/XlibWrapper.class: static native long XMaxRequestSize(long);
sun/awt/X11/XlibWrapper.class: static native long XOpenDisplay(long);
sun/awt/X11/XlibWrapper.class: static native long XScreenNumberOfScreen(long);
sun/awt/X11/XlibWrapper.class: static native void copyIntArray(long, java.lang.Object, int);
sun/awt/X11/XlibWrapper.class: static native void copyLongArray(long, java.lang.Object, int);
sun/awt/X11/XlibWrapper.class: static native void ExitSecondaryLoop();
sun/awt/X11/XlibWrapper.class: static native void memcpy(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void PrintXErrorEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native void SetBitmapShape(long, long, int, int, int[]);
sun/awt/X11/XlibWrapper.class: static native void SetProperty(long, long, long, java.lang.String);
sun/awt/X11/XlibWrapper.class: static native void SetRectangularShape(long, long, int, int, int, int, sun.java2d.pipe.Region);
sun/awt/X11/XlibWrapper.class: static native void SetZOrder(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XBell(long, int);
sun/awt/X11/XlibWrapper.class: static native void XChangeActivePointerGrab(long, int, long, long);
sun/awt/X11/XlibWrapper.class: static native void XChangePropertyImpl(long, long, long, long, int, int, long, int);
sun/awt/X11/XlibWrapper.class: static native void XChangePropertyS(long, long, long, long, int, int, java.lang.String);
sun/awt/X11/XlibWrapper.class: static native void XChangeWindowAttributes(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XClearWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XCloseDisplay(long);
sun/awt/X11/XlibWrapper.class: static native void XConfigureWindow(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XConvertCase(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XConvertSelection(long, long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XDeleteProperty(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XDestroyImage(long);
sun/awt/X11/XlibWrapper.class: static native void XDestroyWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XFlush(long);
sun/awt/X11/XlibWrapper.class: static native void XFreeCursor(long, long);
sun/awt/X11/XlibWrapper.class: static native void XFreeGC(long, long);
sun/awt/X11/XlibWrapper.class: static native void XFree(long);
sun/awt/X11/XlibWrapper.class: static native void XFreeModifiermap(long);
sun/awt/X11/XlibWrapper.class: static native void XFreePixmap(long, long);
sun/awt/X11/XlibWrapper.class: static native void XGetWMHints(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XGrabServer(long);
sun/awt/X11/XlibWrapper.class: static native void XkbFreeKeyboard(long, long, boolean);
sun/awt/X11/XlibWrapper.class: static native void XkbSelectEventDetails(long, long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XkbSelectEvents(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XLowerWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XMapRaised(long, long);
sun/awt/X11/XlibWrapper.class: static native void XMapWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XMaskEvent(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XMoveResizeWindow(long, long, int, int, int, int);
sun/awt/X11/XlibWrapper.class: static native void XMoveWindow(long, long, int, int);
sun/awt/X11/XlibWrapper.class: static native void XNextEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native void XPeekEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native void XPutBackEvent(long, long);
sun/awt/X11/XlibWrapper.class: static native void XPutImage(long, long, long, long, int, int, int, int, int, int);
sun/awt/X11/XlibWrapper.class: static native void XQueryKeymap(long, long);
sun/awt/X11/XlibWrapper.class: static native void XRaiseWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XRefreshKeyboardMapping(long);
sun/awt/X11/XlibWrapper.class: static native void XReparentWindow(long, long, long, int, int);
sun/awt/X11/XlibWrapper.class: static native void XResizeWindow(long, long, int, int);
sun/awt/X11/XlibWrapper.class: static native void XRestackWindows(long, long, int);
sun/awt/X11/XlibWrapper.class: static native void XSelectInput(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetCloseDownMode(long, int);
sun/awt/X11/XlibWrapper.class: static native void XSetErrorHandler(long);
sun/awt/X11/XlibWrapper.class: static native void XSetInputFocus2(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetInputFocus(long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetMinMaxHints(long, long, int, int, int, int, long);
sun/awt/X11/XlibWrapper.class: static native void XSetSelectionOwner(long, long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetTransientFor(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetWindowBackground(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetWindowBackgroundPixmap(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetWMHints(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSetWMNormalHints(long, long, long);
sun/awt/X11/XlibWrapper.class: static native void XSync(long, int);
sun/awt/X11/XlibWrapper.class: static native void XUngrabKeyboard(long, long);
sun/awt/X11/XlibWrapper.class: static native void XUngrabPointer(long, long);
sun/awt/X11/XlibWrapper.class: static native void XUngrabServer(long);
sun/awt/X11/XlibWrapper.class: static native void XUnmapWindow(long, long);
sun/awt/X11/XlibWrapper.class: static native void XWindowEvent(long, long, long, long);
sun/awt/X11/XRobotPeer.class: private static synchronized native void getRGBPixelsImpl(sun.awt.X11GraphicsConfig, int, int, int, int, int[]);
sun/awt/X11/XRobotPeer.class: private static synchronized native void keyPressImpl(int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void keyReleaseImpl(int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void mouseMoveImpl(sun.awt.X11GraphicsConfig, int, int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void mousePressImpl(int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void mouseReleaseImpl(int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void mouseWheelImpl(int);
sun/awt/X11/XRobotPeer.class: private static synchronized native void setup(int, int[]);
sun/awt/X11/XToolkit.class: private native int getNumberOfButtonsImpl();
sun/awt/X11/XToolkit.class: private static native void initIDs();
sun/awt/X11/XToolkit.class: public native void nativeLoadSystemColors(int[]);
sun/awt/X11/XToolkit.class: static native java.lang.String getEnv(java.lang.String);
sun/awt/X11/XToolkit.class: static native long getDefaultScreenData();
sun/awt/X11/XToolkit.class: static native long getDefaultXColormap();
sun/awt/X11/XToolkit.class: static native long getTrayIconDisplayTimeout();
sun/awt/X11/XToolkit.class: static native void awt_output_flush();
sun/awt/X11/XToolkit.class: static native void awt_toolkit_init();
sun/awt/X11/XToolkit.class: static native void waitForEvents(long);
sun/awt/X11/XToolkit.class: static native void wakeup_poll();
sun/awt/X11/XWindow.class: native boolean haveCurrentX11InputMethodInstance();
sun/awt/X11/XWindow.class: native int getNativeColor(java.awt.Color, java.awt.GraphicsConfiguration);
sun/awt/X11/XWindow.class: native long getTopWindow(long, long);
sun/awt/X11/XWindow.class: native void getWindowBounds(long, long, long, long, long);
sun/awt/X11/XWindow.class: native void getWMInsets(long, long, long, long, long, long);
sun/awt/X11/XWindow.class: private static native void initIDs();
sun/awt/X11/XWindow.class: public native boolean x11inputMethodLookupString(long, long[]);
sun/awt/X11/XWindow.class: static native int getAWTKeyCodeForKeySym(int);
sun/awt/X11/XWindow.class: static native int getKeySymForAWTKeyCode(int);
sun/awt/X11/XWindowPeer.class: private static native int getJvmPID();
sun/awt/X11/XWindowPeer.class: private static native java.lang.String getLocalHostname();
sun/font/FileFontStrike.class: private native long _getGlyphImageFromWindows(java.lang.String, int, int, int, boolean);
sun/font/FileFontStrike.class: private static native boolean initNative();
sun/font/FontConfigManager.class: private static native int getFontConfigAASettings(java.lang.String, java.lang.String);
sun/font/FontConfigManager.class: private static native void getFontConfig(java.lang.String, sun.font.FontConfigManager$FontConfigInfo, sun.font.FontConfigManager$FcCompFont[], boolean);
sun/font/FontConfigManager.class: public static native int getFontConfigVersion();
sun/font/FreetypeFontScaler.class: native java.awt.geom.Point2D$Float getGlyphPointNative(sun.font.Font2D, long, long, int, int);
sun/font/FreetypeFontScaler.class: native long createScalerContextNative(long, double[], int, int, float, float);
sun/font/FreetypeFontScaler.class: private native float getGlyphAdvanceNative(sun.font.Font2D, long, long, int);
sun/font/FreetypeFontScaler.class: private native int getGlyphCodeNative(sun.font.Font2D, long, char);
sun/font/FreetypeFontScaler.class: private native int getMissingGlyphCodeNative(long);
sun/font/FreetypeFontScaler.class: private native int getNumGlyphsNative(long);
sun/font/FreetypeFontScaler.class: private native java.awt.geom.GeneralPath getGlyphOutlineNative(sun.font.Font2D, long, long, int, float, float);
sun/font/FreetypeFontScaler.class: private native java.awt.geom.GeneralPath getGlyphVectorOutlineNative(sun.font.Font2D, long, long, int[], int, float, float);
sun/font/FreetypeFontScaler.class: private native java.awt.geom.Rectangle2D$Float getGlyphOutlineBoundsNative(sun.font.Font2D, long, long, int);
sun/font/FreetypeFontScaler.class: private native long getGlyphImageNative(sun.font.Font2D, long, long, int);
sun/font/FreetypeFontScaler.class: private native long getLayoutTableCacheNative(long);
sun/font/FreetypeFontScaler.class: private native long getUnitsPerEMNative(long);
sun/font/FreetypeFontScaler.class: private native long initNativeScaler(sun.font.Font2D, int, int, boolean, int);
sun/font/FreetypeFontScaler.class: private native sun.font.StrikeMetrics getFontMetricsNative(sun.font.Font2D, long, long);
sun/font/FreetypeFontScaler.class: private native void disposeNativeScaler(sun.font.Font2D, long);
sun/font/FreetypeFontScaler.class: private native void getGlyphMetricsNative(sun.font.Font2D, long, long, int, java.awt.geom.Point2D$Float);
sun/font/FreetypeFontScaler.class: private static native void initIDs(java.lang.Class);
sun/font/NativeFont.class: native float getGlyphAdvance(long, int);
sun/font/NativeFont.class: native long getGlyphImage(long, int);
sun/font/NativeFont.class: native long getGlyphImageNoDefault(long, int);
sun/font/NativeFont.class: native sun.font.StrikeMetrics getFontMetrics(long);
sun/font/NativeFont.class: private native int countGlyphs(byte[], int);
sun/font/NativeFont.class: private static native boolean fontExists(byte[]);
sun/font/NativeFont.class: private static native boolean haveBitmapFonts(byte[]);
sun/font/NativeStrike.class: private native int getMaxGlyph(long);
sun/font/NativeStrike.class: private native long createNullScalerContext();
sun/font/NativeStrike.class: private native long createScalerContext(byte[], int, double);
sun/font/NativeStrikeDisposer.class: private native void freeNativeScalerContext(long);
sun/font/NullFontScaler.class: native long getGlyphImage(long, int);
sun/font/NullFontScaler.class: static native long getNullScalerContext();
sun/font/StrikeCache.class: private static native void freeIntMemory(int[], long);
sun/font/StrikeCache.class: private static native void freeLongMemory(long[], long);
sun/font/StrikeCache.class: static native void freeIntPointer(int);
sun/font/StrikeCache.class: static native void freeLongPointer(long);
sun/font/StrikeCache.class: static native void getGlyphCacheDescription(long[]);
sun/font/SunFontManager.class: private static native void initIDs();
sun/font/SunLayoutEngine.class: private static native void initGVIDs();
sun/font/SunLayoutEngine.class: private static native void nativeLayout(sun.font.Font2D, sun.font.FontStrike, float[], int, int, char[], int, int, int, int, int, int, int, java.awt.geom.Point2D$Float, sun.font.GlyphLayout$GVData, long, long);
sun/font/X11TextRenderer.class: native void doDrawGlyphList(long, long, sun.java2d.pipe.Region, sun.font.GlyphList);
sun/instrument/InstrumentationImpl.class: private native boolean isModifiableClass0(long, java.lang.Class<?>);
sun/instrument/InstrumentationImpl.class: private native boolean isRetransformClassesSupported0(long);
sun/instrument/InstrumentationImpl.class: private native java.lang.Class[] getAllLoadedClasses0(long);
sun/instrument/InstrumentationImpl.class: private native java.lang.Class[] getInitiatedClasses0(long, java.lang.ClassLoader);
sun/instrument/InstrumentationImpl.class: private native long getObjectSize0(long, java.lang.Object);
sun/instrument/InstrumentationImpl.class: private native void appendToClassLoaderSearch0(long, java.lang.String, boolean);
sun/instrument/InstrumentationImpl.class: private native void redefineClasses0(long, java.lang.instrument.ClassDefinition[]) throws java.lang.ClassNotFoundException;
sun/instrument/InstrumentationImpl.class: private native void retransformClasses0(long, java.lang.Class<?>[]);
sun/instrument/InstrumentationImpl.class: private native void setHasRetransformableTransformers(long, boolean);
sun/instrument/InstrumentationImpl.class: private native void setNativeMethodPrefixes(long, java.lang.String[], boolean);
sun/invoke/anon/AnonymousClassLoader.class: private static native java.lang.Class<?> loadClassInternal(java.lang.Class<?>, byte[], java.lang.Object[]);
sun/java2d/cmm/lcms/LCMS.class: public native long loadProfile(byte[]);
sun/java2d/cmm/lcms/LCMS.class: public native void freeProfile(long);
sun/java2d/cmm/lcms/LCMS.class: public static native long createNativeTransform(long[], int, int, int, java.lang.Object);
sun/java2d/cmm/lcms/LCMS.class: public static native long getProfileID(java.awt.color.ICC_Profile);
sun/java2d/cmm/lcms/LCMS.class: public static native void colorConvert(sun.java2d.cmm.lcms.LCMSTransform, sun.java2d.cmm.lcms.LCMSImageLayout, sun.java2d.cmm.lcms.LCMSImageLayout);
sun/java2d/cmm/lcms/LCMS.class: public static native void freeTransform(long);
sun/java2d/cmm/lcms/LCMS.class: public static native void initLCMS(java.lang.Class, java.lang.Class, java.lang.Class);
sun/java2d/cmm/lcms/LCMS.class: public synchronized native int getProfileSize(long);
sun/java2d/cmm/lcms/LCMS.class: public synchronized native int getTagSize(long, int);
sun/java2d/cmm/lcms/LCMS.class: public synchronized native void getProfileData(long, byte[]);
sun/java2d/cmm/lcms/LCMS.class: public synchronized native void getTagData(long, int, byte[]);
sun/java2d/cmm/lcms/LCMS.class: public synchronized native void setTagData(long, int, byte[]);
sun/java2d/DefaultDisposerRecord.class: public static native void invokeNativeDispose(long, long);
sun/java2d/Disposer.class: private static native void initIDs();
sun/java2d/jules/JulesAATileGenerator.class: private static native long rasterizeTrapezoidsNative(long, int[], int[], int, byte[], int, int);
sun/java2d/jules/JulesAATileGenerator.class: private static native void freePixmanImgPtr(long);
sun/java2d/jules/JulesPathBuf.class: private static native int[] tesselateFillNative(int[], byte[], int, int, int[], int, int, int, int, int, int);
sun/java2d/jules/JulesPathBuf.class: private static native int[] tesselateStrokeNative(int[], byte[], int, int, int[], int, double, int, int, double, double[], int, double, double, double, double, double, double, double, int, int, int, int);
sun/java2d/loops/BlitBg.class: public native void BlitBg(sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, int, int, int, int, int, int, int);
sun/java2d/loops/Blit.class: public native void Blit(sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, int, int, int, int, int, int);
sun/java2d/loops/DrawGlyphListAA.class: public native void DrawGlyphListAA(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, sun.font.GlyphList);
sun/java2d/loops/DrawGlyphList.class: public native void DrawGlyphList(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, sun.font.GlyphList);
sun/java2d/loops/DrawGlyphListLCD.class: public native void DrawGlyphListLCD(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, sun.font.GlyphList);
sun/java2d/loops/DrawLine.class: public native void DrawLine(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, int, int, int);
sun/java2d/loops/DrawParallelogram.class: public native void DrawParallelogram(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, double, double, double, double, double, double, double, double);
sun/java2d/loops/DrawPath.class: public native void DrawPath(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, int, java.awt.geom.Path2D$Float);
sun/java2d/loops/DrawPolygons.class: public native void DrawPolygons(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int[], int[], int[], int, int, int, boolean);
sun/java2d/loops/DrawRect.class: public native void DrawRect(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, int, int, int);
sun/java2d/loops/FillParallelogram.class: public native void FillParallelogram(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, double, double, double, double, double, double);
sun/java2d/loops/FillPath.class: public native void FillPath(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, int, java.awt.geom.Path2D$Float);
sun/java2d/loops/FillRect.class: public native void FillRect(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, int, int, int);
sun/java2d/loops/FillSpans.class: private native void FillSpans(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, int, long, sun.java2d.pipe.SpanIterator);
sun/java2d/loops/GraphicsPrimitiveMgr.class: private static native void initIDs(java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class);
sun/java2d/loops/GraphicsPrimitiveMgr.class: private static native void registerNativeLoops();
sun/java2d/loops/MaskBlit.class: public native void MaskBlit(sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, int, int, int, int, int, int, byte[], int, int);
sun/java2d/loops/MaskFill.class: public native void DrawAAPgram(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, java.awt.Composite, double, double, double, double, double, double, double, double);
sun/java2d/loops/MaskFill.class: public native void FillAAPgram(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, java.awt.Composite, double, double, double, double, double, double);
sun/java2d/loops/MaskFill.class: public native void MaskFill(sun.java2d.SunGraphics2D, sun.java2d.SurfaceData, java.awt.Composite, int, int, int, int, byte[], int, int);
sun/java2d/loops/ScaledBlit.class: public native void Scale(sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, int, int, int, int, double, double, double, double);
sun/java2d/loops/TransformBlit.class: public native void Transform(sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, java.awt.geom.AffineTransform, int, int, int, int, int, int, int);
sun/java2d/loops/TransformHelper.class: public native void Transform(sun.java2d.loops.MaskBlit, sun.java2d.SurfaceData, sun.java2d.SurfaceData, java.awt.Composite, sun.java2d.pipe.Region, java.awt.geom.AffineTransform, int, int, int, int, int, int, int, int, int, int[], int, int);
sun/java2d/opengl/GLXGraphicsConfig.class: private native void initConfig(long, long);
sun/java2d/opengl/GLXGraphicsConfig.class: private static native int getOGLCapabilities(long);
sun/java2d/opengl/GLXGraphicsConfig.class: private static native long getGLXConfigInfo(int, int);
sun/java2d/opengl/GLXSurfaceData.class: private native void initOps(sun.awt.X11ComponentPeer, long);
sun/java2d/opengl/GLXSurfaceData.class: protected native boolean initPbuffer(long, long, boolean, int, int);
sun/java2d/opengl/OGLContext.class: static final native java.lang.String getOGLIdString();
sun/java2d/opengl/OGLMaskFill.class: protected native void maskFill(int, int, int, int, int, int, int, byte[]);
sun/java2d/opengl/OGLRenderer.class: protected native void drawPoly(int[], int[], int, boolean, int, int);
sun/java2d/opengl/OGLRenderQueue.class: private native void flushBuffer(long, int);
sun/java2d/opengl/OGLSurfaceData.class: private native int getTextureID(long);
sun/java2d/opengl/OGLSurfaceData.class: private native int getTextureTarget(long);
sun/java2d/opengl/OGLSurfaceData.class: protected native boolean initFBObject(long, boolean, boolean, boolean, int, int);
sun/java2d/opengl/OGLSurfaceData.class: protected native boolean initFlipBackbuffer(long);
sun/java2d/opengl/OGLSurfaceData.class: protected native boolean initTexture(long, boolean, boolean, boolean, int, int);
sun/java2d/opengl/OGLTextRenderer.class: protected native void drawGlyphList(int, boolean, boolean, boolean, int, float, float, long[], float[]);
sun/java2d/pipe/BufferedMaskBlit.class: private native int enqueueTile(long, int, sun.java2d.SurfaceData, long, int, byte[], int, int, int, int, int, int, int, int, int);
sun/java2d/pipe/BufferedRenderPipe.class: private native int fillSpans(sun.java2d.pipe.RenderQueue, long, int, int, sun.java2d.pipe.SpanIterator, long, int, int);
sun/java2d/pipe/Region.class: private static native void initIDs();
sun/java2d/pipe/ShapeSpanIterator.class: private native void setNormalize(boolean);
sun/java2d/pipe/ShapeSpanIterator.class: public native boolean nextSpan(int[]);
sun/java2d/pipe/ShapeSpanIterator.class: public native long getNativeConsumer();
sun/java2d/pipe/ShapeSpanIterator.class: public native long getNativeIterator();
sun/java2d/pipe/ShapeSpanIterator.class: public native void addSegment(int, float[]);
sun/java2d/pipe/ShapeSpanIterator.class: public native void appendPoly(int[], int[], int, int, int);
sun/java2d/pipe/ShapeSpanIterator.class: public native void closePath();
sun/java2d/pipe/ShapeSpanIterator.class: public native void curveTo(float, float, float, float, float, float);
sun/java2d/pipe/ShapeSpanIterator.class: public native void dispose();
sun/java2d/pipe/ShapeSpanIterator.class: public native void getPathBox(int[]);
sun/java2d/pipe/ShapeSpanIterator.class: public native void intersectClipBox(int, int, int, int);
sun/java2d/pipe/ShapeSpanIterator.class: public native void lineTo(float, float);
sun/java2d/pipe/ShapeSpanIterator.class: public native void moveTo(float, float);
sun/java2d/pipe/ShapeSpanIterator.class: public native void pathDone();
sun/java2d/pipe/ShapeSpanIterator.class: public native void quadTo(float, float, float, float);
sun/java2d/pipe/ShapeSpanIterator.class: public native void setOutputAreaXYXY(int, int, int, int);
sun/java2d/pipe/ShapeSpanIterator.class: public native void setRule(int);
sun/java2d/pipe/ShapeSpanIterator.class: public native void skipDownTo(int);
sun/java2d/pipe/ShapeSpanIterator.class: public static native void initIDs();
sun/java2d/pipe/SpanClipRenderer.class: public native void eraseTile(sun.java2d.pipe.RegionIterator, byte[], int, int, int[]);
sun/java2d/pipe/SpanClipRenderer.class: public native void fillTile(sun.java2d.pipe.RegionIterator, byte[], int, int, int[]);
sun/java2d/pipe/SpanClipRenderer.class: static native void initIDs(java.lang.Class, java.lang.Class);
sun/java2d/SurfaceData.class: private static native void initIDs();
sun/java2d/SurfaceData.class: protected static native boolean isOpaqueGray(java.awt.image.IndexColorModel);
sun/java2d/x11/X11PMBlitBgLoops.class: private native void nativeBlitBg(long, long, long, int, int, int, int, int, int, int);
sun/java2d/x11/X11PMBlitLoops.class: private native void nativeBlit(long, long, long, sun.java2d.pipe.Region, int, int, int, int, int, int);
sun/java2d/x11/X11PMBlitLoops.class: private static native void updateBitmask(sun.java2d.SurfaceData, sun.java2d.SurfaceData, boolean);
sun/java2d/x11/X11Renderer.class: native void devCopyArea(long, long, int, int, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XDoPath(sun.java2d.SunGraphics2D, long, long, int, int, java.awt.geom.Path2D$Float, boolean);
sun/java2d/x11/X11Renderer.class: native void XDrawArc(long, long, int, int, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XDrawLine(long, long, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XDrawOval(long, long, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XDrawPoly(long, long, int, int, int[], int[], int, boolean);
sun/java2d/x11/X11Renderer.class: native void XDrawRect(long, long, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XDrawRoundRect(long, long, int, int, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XFillArc(long, long, int, int, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XFillOval(long, long, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XFillPoly(long, long, int, int, int[], int[], int);
sun/java2d/x11/X11Renderer.class: native void XFillRect(long, long, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XFillRoundRect(long, long, int, int, int, int, int, int);
sun/java2d/x11/X11Renderer.class: native void XFillSpans(long, long, sun.java2d.pipe.SpanIterator, long, int, int);
sun/java2d/x11/X11SurfaceData.class: private static native boolean isShmPMAvailable();
sun/java2d/x11/X11SurfaceData.class: private static native void initIDs(java.lang.Class, boolean);
sun/java2d/x11/X11SurfaceData.class: private static native void XSetCopyMode(long);
sun/java2d/x11/X11SurfaceData.class: private static native void XSetForeground(long, int);
sun/java2d/x11/X11SurfaceData.class: private static native void XSetXorMode(long);
sun/java2d/x11/X11SurfaceData.class: protected native void initSurface(int, int, int, long);
sun/java2d/x11/X11SurfaceData.class: public static native boolean isDgaAvailable();
sun/java2d/x11/XSurfaceData.class: protected native boolean isDrawableValid();
sun/java2d/x11/XSurfaceData.class: protected native void flushNativeSurface();
sun/java2d/x11/XSurfaceData.class: protected native void initOps(sun.awt.X11ComponentPeer, sun.awt.X11GraphicsConfig, int);
sun/java2d/x11/XSurfaceData.class: protected native void setInvalid();
sun/java2d/x11/XSurfaceData.class: protected static native long XCreateGC(long);
sun/java2d/x11/XSurfaceData.class: protected static native void XResetClip(long);
sun/java2d/x11/XSurfaceData.class: protected static native void XSetClip(long, int, int, int, int, sun.java2d.pipe.Region);
sun/java2d/x11/XSurfaceData.class: protected static native void XSetGraphicsExposures(long, boolean);
sun/java2d/xr/XIDGenerator.class: private static native void bufferXIDs(int[], int);
sun/java2d/xr/XRBackendNative.class: private native int createPictureNative(int, long);
sun/java2d/xr/XRBackendNative.class: private native void renderRectangle(int, byte, short, short, short, short, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: private native void XRSetTransformNative(int, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: private static native int XRCreateLinearGradientPaintNative(float[], short[], int, int, int, int, int, int, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: private static native int XRCreateRadialGradientPaintNative(float[], short[], int, int, int, int, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: private static native int XRenderCreateGlyphSetNative(long);
sun/java2d/xr/XRBackendNative.class: private static native void GCRectanglesNative(int, long, int[], int);
sun/java2d/xr/XRBackendNative.class: private static native void initIDs();
sun/java2d/xr/XRBackendNative.class: private static native void padBlitNative(byte, int, int, int, int, int, int, int, int, int, int, int, int, int, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: private static native void putMaskNative(int, long, byte[], int, int, int, int, int, int, int, int, float, long);
sun/java2d/xr/XRBackendNative.class: private static native void renderCompositeTrapezoidsNative(byte, int, long, int, int, int, int[]);
sun/java2d/xr/XRBackendNative.class: private static native void XRAddGlyphsNative(int, long[], int, byte[], int);
sun/java2d/xr/XRBackendNative.class: private static native void XRenderCompositeTextNative(int, int, int, long, int[], int[], int, int);
sun/java2d/xr/XRBackendNative.class: private static native void XRenderRectanglesNative(int, byte, short, short, short, short, int[], int);
sun/java2d/xr/XRBackendNative.class: private static native void XRFreeGlyphsNative(int, int[], int);
sun/java2d/xr/XRBackendNative.class: private static native void XRSetClipNative(long, int, int, int, int, sun.java2d.pipe.Region, boolean);
sun/java2d/xr/XRBackendNative.class: public native int createPixmap(int, int, int, int);
sun/java2d/xr/XRBackendNative.class: public native long createGC(int);
sun/java2d/xr/XRBackendNative.class: public native void copyArea(int, int, long, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: public native void freeGC(long);
sun/java2d/xr/XRBackendNative.class: public native void freePicture(int);
sun/java2d/xr/XRBackendNative.class: public native void freePixmap(int);
sun/java2d/xr/XRBackendNative.class: public native void renderComposite(byte, int, int, int, int, int, int, int, int, int, int, int);
sun/java2d/xr/XRBackendNative.class: public native void setFilter(int, int);
sun/java2d/xr/XRBackendNative.class: public native void setGCExposures(long, boolean);
sun/java2d/xr/XRBackendNative.class: public native void setGCForeground(long, int);
sun/java2d/xr/XRBackendNative.class: public native void setGCMode(long, boolean);
sun/java2d/xr/XRBackendNative.class: public native void setPictureRepeat(int, int);
sun/java2d/xr/XRMaskBlit.class: protected native void maskBlit(long, long, int, int, int, int, int, int, int, int, int, byte[]);
sun/java2d/xr/XRMaskFill.class: protected native void maskFill(long, int, int, int, int, int, int, int, byte[]);
sun/java2d/xr/XRSurfaceData.class: native void freeXSDOPicture(long);
sun/java2d/xr/XRSurfaceData.class: native void initXRPicture(long, int);
sun/java2d/xr/XRSurfaceData.class: private static native void initIDs();
sun/java2d/xr/XRSurfaceData.class: protected native void XRInitSurface(int, int, int, long, int);
sun/management/ClassLoadingImpl.class: static native void setVerboseClass(boolean);
sun/management/FileSystemImpl.class: static native boolean isAccessUserOnly0(java.lang.String) throws java.io.IOException;
sun/management/Flag.class: private static native int getFlags(java.lang.String[], sun.management.Flag[], int);
sun/management/Flag.class: private static native int getInternalFlagCount();
sun/management/Flag.class: private static native java.lang.String[] getAllFlagNames();
sun/management/Flag.class: private static native void initialize();
sun/management/Flag.class: static synchronized native void setBooleanValue(java.lang.String, boolean);
sun/management/Flag.class: static synchronized native void setLongValue(java.lang.String, long);
sun/management/Flag.class: static synchronized native void setStringValue(java.lang.String, java.lang.String);
sun/management/GarbageCollectorImpl.class: native void setNotificationEnabled(com.sun.management.GarbageCollectorMXBean, boolean);
sun/management/GarbageCollectorImpl.class: public native long getCollectionCount();
sun/management/GarbageCollectorImpl.class: public native long getCollectionTime();
sun/management/GcInfoBuilder.class: private native com.sun.management.GcInfo getLastGcInfo0(java.lang.management.GarbageCollectorMXBean, int, java.lang.Object[], char[], java.lang.management.MemoryUsage[], java.lang.management.MemoryUsage[]);
sun/management/GcInfoBuilder.class: private native int getNumGcExtAttributes(java.lang.management.GarbageCollectorMXBean);
sun/management/GcInfoBuilder.class: private native void fillGcAttributeInfo(java.lang.management.GarbageCollectorMXBean, int, java.lang.String[], char[], java.lang.String[]);
sun/management/HotSpotDiagnostic.class: private native void dumpHeap0(java.lang.String, boolean) throws java.io.IOException;
sun/management/HotspotThread.class: public native int getInternalThreadCount();
sun/management/HotspotThread.class: public native int getInternalThreadTimes0(java.lang.String[], long[]);
sun/management/MemoryImpl.class: private native java.lang.management.MemoryUsage getMemoryUsage0(boolean);
sun/management/MemoryImpl.class: private native void setVerboseGC(boolean);
sun/management/MemoryImpl.class: private static native java.lang.management.MemoryManagerMXBean[] getMemoryManagers0();
sun/management/MemoryImpl.class: private static native java.lang.management.MemoryPoolMXBean[] getMemoryPools0();
sun/management/MemoryManagerImpl.class: private native java.lang.management.MemoryPoolMXBean[] getMemoryPools0();
sun/management/MemoryPoolImpl.class: private native java.lang.management.MemoryManagerMXBean[] getMemoryManagers0();
sun/management/MemoryPoolImpl.class: private native java.lang.management.MemoryUsage getCollectionUsage0();
sun/management/MemoryPoolImpl.class: private native java.lang.management.MemoryUsage getPeakUsage0();
sun/management/MemoryPoolImpl.class: private native java.lang.management.MemoryUsage getUsage0();
sun/management/MemoryPoolImpl.class: private native void resetPeakUsage0();
sun/management/MemoryPoolImpl.class: private native void setCollectionThreshold0(long, long);
sun/management/MemoryPoolImpl.class: private native void setPoolCollectionSensor(sun.management.Sensor);
sun/management/MemoryPoolImpl.class: private native void setPoolUsageSensor(sun.management.Sensor);
sun/management/MemoryPoolImpl.class: private native void setUsageThreshold0(long, long);
sun/management/ThreadImpl.class: private static native java.lang.management.ThreadInfo[] dumpThreads0(long[], boolean, boolean);
sun/management/ThreadImpl.class: private static native java.lang.Thread[] findDeadlockedThreads0();
sun/management/ThreadImpl.class: private static native java.lang.Thread[] findMonitorDeadlockedThreads0();
sun/management/ThreadImpl.class: private static native java.lang.Thread[] getThreads();
sun/management/ThreadImpl.class: private static native long getThreadTotalCpuTime0(long);
sun/management/ThreadImpl.class: private static native long getThreadUserCpuTime0(long);
sun/management/ThreadImpl.class: private static native void getThreadAllocatedMemory1(long[], long[]);
sun/management/ThreadImpl.class: private static native void getThreadInfo1(long[], int, java.lang.management.ThreadInfo[]);
sun/management/ThreadImpl.class: private static native void getThreadTotalCpuTime1(long[], long[]);
sun/management/ThreadImpl.class: private static native void getThreadUserCpuTime1(long[], long[]);
sun/management/ThreadImpl.class: private static native void resetContentionTimes0(long);
sun/management/ThreadImpl.class: private static native void resetPeakThreadCount0();
sun/management/ThreadImpl.class: private static native void setThreadAllocatedMemoryEnabled0(boolean);
sun/management/ThreadImpl.class: private static native void setThreadContentionMonitoringEnabled0(boolean);
sun/management/ThreadImpl.class: private static native void setThreadCpuTimeEnabled0(boolean);
sun/management/VMManagementImpl.class: private native int getProcessId();
sun/management/VMManagementImpl.class: private static native java.lang.String getVersion0();
sun/management/VMManagementImpl.class: private static native void initOptionalSupportFields();
sun/management/VMManagementImpl.class: public native boolean getVerboseClass();
sun/management/VMManagementImpl.class: public native boolean getVerboseGC();
sun/management/VMManagementImpl.class: public native boolean isThreadAllocatedMemoryEnabled();
sun/management/VMManagementImpl.class: public native boolean isThreadContentionMonitoringEnabled();
sun/management/VMManagementImpl.class: public native boolean isThreadCpuTimeEnabled();
sun/management/VMManagementImpl.class: public native int getAvailableProcessors();
sun/management/VMManagementImpl.class: public native int getDaemonThreadCount();
sun/management/VMManagementImpl.class: public native int getLiveThreadCount();
sun/management/VMManagementImpl.class: public native int getPeakThreadCount();
sun/management/VMManagementImpl.class: public native java.lang.String[] getVmArguments0();
sun/management/VMManagementImpl.class: public native long getClassInitializationTime();
sun/management/VMManagementImpl.class: public native long getClassLoadingTime();
sun/management/VMManagementImpl.class: public native long getClassVerificationTime();
sun/management/VMManagementImpl.class: public native long getInitializedClassCount();
sun/management/VMManagementImpl.class: public native long getLoadedClassSize();
sun/management/VMManagementImpl.class: public native long getMethodDataSize();
sun/management/VMManagementImpl.class: public native long getSafepointCount();
sun/management/VMManagementImpl.class: public native long getSafepointSyncTime();
sun/management/VMManagementImpl.class: public native long getStartupTime();
sun/management/VMManagementImpl.class: public native long getTotalApplicationNonStoppedTime();
sun/management/VMManagementImpl.class: public native long getTotalClassCount();
sun/management/VMManagementImpl.class: public native long getTotalCompileTime();
sun/management/VMManagementImpl.class: public native long getTotalSafepointTime();
sun/management/VMManagementImpl.class: public native long getTotalThreadCount();
sun/management/VMManagementImpl.class: public native long getUnloadedClassCount();
sun/management/VMManagementImpl.class: public native long getUnloadedClassSize();
sun/misc/GC.class: public static native long maxObjectInspectionAge();
sun/misc/MessageUtils.class: public static native void toStderr(java.lang.String);
sun/misc/MessageUtils.class: public static native void toStdout(java.lang.String);
sun/misc/NativeSignalHandler.class: private static native void handle0(int, long);
sun/misc/Perf.class: private native java.nio.ByteBuffer attach(java.lang.String, int, int) throws java.lang.IllegalArgumentException, java.io.IOException;
sun/misc/Perf.class: private native void detach(java.nio.ByteBuffer);
sun/misc/Perf.class: private static native void registerNatives();
sun/misc/Perf.class: public native java.nio.ByteBuffer createByteArray(java.lang.String, int, int, byte[], int);
sun/misc/Perf.class: public native java.nio.ByteBuffer createLong(java.lang.String, int, int, long);
sun/misc/Perf.class: public native long highResCounter();
sun/misc/Perf.class: public native long highResFrequency();
sun/misc/Signal.class: private static native int findSignal(java.lang.String);
sun/misc/Signal.class: private static native long handle0(int, long);
sun/misc/Signal.class: private static native void raise0(int);
sun/misc/Unsafe.class: private static native void registerNatives();
sun/misc/Unsafe.class: public final native boolean compareAndSwapInt(java.lang.Object, long, int, int);
sun/misc/Unsafe.class: public final native boolean compareAndSwapLong(java.lang.Object, long, long, long);
sun/misc/Unsafe.class: public final native boolean compareAndSwapObject(java.lang.Object, long, java.lang.Object, java.lang.Object);
sun/misc/Unsafe.class: public native boolean getBoolean(java.lang.Object, long);
sun/misc/Unsafe.class: public native boolean getBooleanVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native boolean shouldBeInitialized(java.lang.Class<?>);
sun/misc/Unsafe.class: public native boolean tryMonitorEnter(java.lang.Object);
sun/misc/Unsafe.class: public native byte getByte(java.lang.Object, long);
sun/misc/Unsafe.class: public native byte getByte(long);
sun/misc/Unsafe.class: public native byte getByteVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native char getChar(java.lang.Object, long);
sun/misc/Unsafe.class: public native char getChar(long);
sun/misc/Unsafe.class: public native char getCharVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native double getDouble(java.lang.Object, long);
sun/misc/Unsafe.class: public native double getDouble(long);
sun/misc/Unsafe.class: public native double getDoubleVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native float getFloat(java.lang.Object, long);
sun/misc/Unsafe.class: public native float getFloat(long);
sun/misc/Unsafe.class: public native float getFloatVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native int addressSize();
sun/misc/Unsafe.class: public native int arrayBaseOffset(java.lang.Class);
sun/misc/Unsafe.class: public native int arrayIndexScale(java.lang.Class);
sun/misc/Unsafe.class: public native int getInt(java.lang.Object, long);
sun/misc/Unsafe.class: public native int getInt(long);
sun/misc/Unsafe.class: public native int getIntVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native int getLoadAverage(double[], int);
sun/misc/Unsafe.class: public native int pageSize();
sun/misc/Unsafe.class: public native java.lang.Class defineAnonymousClass(java.lang.Class, byte[], java.lang.Object[]);
sun/misc/Unsafe.class: public native java.lang.Class defineClass(java.lang.String, byte[], int, int);
sun/misc/Unsafe.class: public native java.lang.Class defineClass(java.lang.String, byte[], int, int, java.lang.ClassLoader, java.security.ProtectionDomain);
sun/misc/Unsafe.class: public native java.lang.Object allocateInstance(java.lang.Class) throws java.lang.InstantiationException;
sun/misc/Unsafe.class: public native java.lang.Object getObject(java.lang.Object, long);
sun/misc/Unsafe.class: public native java.lang.Object getObjectVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native java.lang.Object staticFieldBase(java.lang.reflect.Field);
sun/misc/Unsafe.class: public native long allocateMemory(long);
sun/misc/Unsafe.class: public native long getAddress(long);
sun/misc/Unsafe.class: public native long getLong(java.lang.Object, long);
sun/misc/Unsafe.class: public native long getLong(long);
sun/misc/Unsafe.class: public native long getLongVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native long objectFieldOffset(java.lang.reflect.Field);
sun/misc/Unsafe.class: public native long reallocateMemory(long, long);
sun/misc/Unsafe.class: public native long staticFieldOffset(java.lang.reflect.Field);
sun/misc/Unsafe.class: public native short getShort(java.lang.Object, long);
sun/misc/Unsafe.class: public native short getShort(long);
sun/misc/Unsafe.class: public native short getShortVolatile(java.lang.Object, long);
sun/misc/Unsafe.class: public native void copyMemory(java.lang.Object, long, java.lang.Object, long, long);
sun/misc/Unsafe.class: public native void ensureClassInitialized(java.lang.Class);
sun/misc/Unsafe.class: public native void freeMemory(long);
sun/misc/Unsafe.class: public native void monitorEnter(java.lang.Object);
sun/misc/Unsafe.class: public native void monitorExit(java.lang.Object);
sun/misc/Unsafe.class: public native void park(boolean, long);
sun/misc/Unsafe.class: public native void putAddress(long, long);
sun/misc/Unsafe.class: public native void putBoolean(java.lang.Object, long, boolean);
sun/misc/Unsafe.class: public native void putBooleanVolatile(java.lang.Object, long, boolean);
sun/misc/Unsafe.class: public native void putByte(java.lang.Object, long, byte);
sun/misc/Unsafe.class: public native void putByte(long, byte);
sun/misc/Unsafe.class: public native void putByteVolatile(java.lang.Object, long, byte);
sun/misc/Unsafe.class: public native void putChar(java.lang.Object, long, char);
sun/misc/Unsafe.class: public native void putChar(long, char);
sun/misc/Unsafe.class: public native void putCharVolatile(java.lang.Object, long, char);
sun/misc/Unsafe.class: public native void putDouble(java.lang.Object, long, double);
sun/misc/Unsafe.class: public native void putDouble(long, double);
sun/misc/Unsafe.class: public native void putDoubleVolatile(java.lang.Object, long, double);
sun/misc/Unsafe.class: public native void putFloat(java.lang.Object, long, float);
sun/misc/Unsafe.class: public native void putFloat(long, float);
sun/misc/Unsafe.class: public native void putFloatVolatile(java.lang.Object, long, float);
sun/misc/Unsafe.class: public native void putInt(java.lang.Object, long, int);
sun/misc/Unsafe.class: public native void putInt(long, int);
sun/misc/Unsafe.class: public native void putIntVolatile(java.lang.Object, long, int);
sun/misc/Unsafe.class: public native void putLong(java.lang.Object, long, long);
sun/misc/Unsafe.class: public native void putLong(long, long);
sun/misc/Unsafe.class: public native void putLongVolatile(java.lang.Object, long, long);
sun/misc/Unsafe.class: public native void putObject(java.lang.Object, long, java.lang.Object);
sun/misc/Unsafe.class: public native void putObjectVolatile(java.lang.Object, long, java.lang.Object);
sun/misc/Unsafe.class: public native void putOrderedInt(java.lang.Object, long, int);
sun/misc/Unsafe.class: public native void putOrderedLong(java.lang.Object, long, long);
sun/misc/Unsafe.class: public native void putOrderedObject(java.lang.Object, long, java.lang.Object);
sun/misc/Unsafe.class: public native void putShort(java.lang.Object, long, short);
sun/misc/Unsafe.class: public native void putShort(long, short);
sun/misc/Unsafe.class: public native void putShortVolatile(java.lang.Object, long, short);
sun/misc/Unsafe.class: public native void setMemory(java.lang.Object, long, long, byte);
sun/misc/Unsafe.class: public native void throwException(java.lang.Throwable);
sun/misc/Unsafe.class: public native void unpark(java.lang.Object);
sun/misc/Version.class: private static native boolean getJvmVersionInfo();
sun/misc/Version.class: private static native void getJdkVersionInfo();
sun/misc/Version.class: public static native java.lang.String getJdkSpecialVersion();
sun/misc/Version.class: public static native java.lang.String getJvmSpecialVersion();
sun/misc/VM.class: private static native void initialize();
sun/misc/VM.class: public static native java.lang.ClassLoader latestUserDefinedLoader();
sun/misc/VMSupport.class: private static native java.util.Properties initAgentProperties(java.util.Properties);
sun/misc/VMSupport.class: public static native java.lang.String getVMTemporaryDirectory();
sun/net/dns/ResolverConfigurationImpl.class: static native java.lang.String fallbackDomain0();
sun/net/dns/ResolverConfigurationImpl.class: static native java.lang.String localDomain0();
sun/net/ExtendedOptionsImpl.class: private static native void init();
sun/net/ExtendedOptionsImpl.class: public static native boolean flowSupported();
sun/net/ExtendedOptionsImpl.class: public static native void getFlowOption(java.io.FileDescriptor, jdk.net.SocketFlow);
sun/net/ExtendedOptionsImpl.class: public static native void setFlowOption(java.io.FileDescriptor, jdk.net.SocketFlow);
sun/net/PortConfig.class: static native int getLower0();
sun/net/PortConfig.class: static native int getUpper0();
sun/net/sdp/SdpSupport.class: private static native int create0() throws java.io.IOException;
sun/net/sdp/SdpSupport.class: private static native void convert0(int) throws java.io.IOException;
sun/net/spi/DefaultProxySelector.class: private static native boolean init();
sun/net/spi/DefaultProxySelector.class: private synchronized native java.net.Proxy getSystemProxy(java.lang.String, java.lang.String);
sun/nio/ch/AixPollPort.class: private static native int eventSize();
sun/nio/ch/AixPollPort.class: private static native int eventsOffset();
sun/nio/ch/AixPollPort.class: private static native int fdOffset();
sun/nio/ch/AixPollPort.class: private static native int pollsetCreate() throws java.io.IOException;
sun/nio/ch/AixPollPort.class: private static native int pollsetCtl(int, int, int, int);
sun/nio/ch/AixPollPort.class: private static native int pollsetPoll(int, long, int) throws java.io.IOException;
sun/nio/ch/AixPollPort.class: private static native int reventsOffset();
sun/nio/ch/AixPollPort.class: private static native void close0(int);
sun/nio/ch/AixPollPort.class: private static native void drain1(int) throws java.io.IOException;
sun/nio/ch/AixPollPort.class: private static native void init();
sun/nio/ch/AixPollPort.class: private static native void interrupt(int) throws java.io.IOException;
sun/nio/ch/AixPollPort.class: private static native void pollsetDestroy(int);
sun/nio/ch/AixPollPort.class: private static native void socketpair(int[]) throws java.io.IOException;
sun/nio/ch/DatagramChannelImpl.class: private native int receive0(java.io.FileDescriptor, long, int, boolean) throws java.io.IOException;
sun/nio/ch/DatagramChannelImpl.class: private native int send0(boolean, java.io.FileDescriptor, long, int, java.net.InetAddress, int) throws java.io.IOException;
sun/nio/ch/DatagramChannelImpl.class: private static native void disconnect0(java.io.FileDescriptor, boolean) throws java.io.IOException;
sun/nio/ch/DatagramChannelImpl.class: private static native void initIDs();
sun/nio/ch/DatagramDispatcher.class: static native int read0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/DatagramDispatcher.class: static native int write0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/DatagramDispatcher.class: static native long readv0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/DatagramDispatcher.class: static native long writev0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/DevPollArrayWrapper.class: private native int init();
sun/nio/ch/DevPollArrayWrapper.class: private native int poll0(long, int, long, int);
sun/nio/ch/DevPollArrayWrapper.class: private native void register(int, int, int);
sun/nio/ch/DevPollArrayWrapper.class: private native void registerMultiple(int, long, int) throws java.io.IOException;
sun/nio/ch/DevPollArrayWrapper.class: private static native void interrupt(int);
sun/nio/ch/EPollArrayWrapper.class: private native int epollCreate();
sun/nio/ch/EPollArrayWrapper.class: private native int epollWait(long, int, long, int) throws java.io.IOException;
sun/nio/ch/EPollArrayWrapper.class: private native void epollCtl(int, int, int, int);
sun/nio/ch/EPollArrayWrapper.class: private static native int offsetofData();
sun/nio/ch/EPollArrayWrapper.class: private static native int sizeofEPollEvent();
sun/nio/ch/EPollArrayWrapper.class: private static native void init();
sun/nio/ch/EPollArrayWrapper.class: private static native void interrupt(int);
sun/nio/ch/EPoll.class: private static native int dataOffset();
sun/nio/ch/EPoll.class: private static native int eventSize();
sun/nio/ch/EPoll.class: private static native int eventsOffset();
sun/nio/ch/EPoll.class: static native int epollCreate() throws java.io.IOException;
sun/nio/ch/EPoll.class: static native int epollCtl(int, int, int, int);
sun/nio/ch/EPoll.class: static native int epollWait(int, long, int) throws java.io.IOException;
sun/nio/ch/EPollPort.class: private static native void close0(int);
sun/nio/ch/EPollPort.class: private static native void drain1(int) throws java.io.IOException;
sun/nio/ch/EPollPort.class: private static native void interrupt(int) throws java.io.IOException;
sun/nio/ch/EPollPort.class: private static native void socketpair(int[]) throws java.io.IOException;
sun/nio/ch/FileChannelImpl.class: private native long map0(int, long, long) throws java.io.IOException;
sun/nio/ch/FileChannelImpl.class: private native long position0(java.io.FileDescriptor, long);
sun/nio/ch/FileChannelImpl.class: private native long transferTo0(java.io.FileDescriptor, long, long, java.io.FileDescriptor);
sun/nio/ch/FileChannelImpl.class: private static native int unmap0(long, long);
sun/nio/ch/FileChannelImpl.class: private static native long initIDs();
sun/nio/ch/FileDispatcherImpl.class: static native int force0(java.io.FileDescriptor, boolean, boolean) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int lock0(java.io.FileDescriptor, boolean, long, long, boolean) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int pread0(java.io.FileDescriptor, long, int, long) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int pwrite0(java.io.FileDescriptor, long, int, long) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int read0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int truncate0(java.io.FileDescriptor, long) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native int write0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native long readv0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native long size0(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native long writev0(java.io.FileDescriptor, long, int) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native void close0(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native void closeIntFD(int) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native void init();
sun/nio/ch/FileDispatcherImpl.class: static native void preClose0(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/FileDispatcherImpl.class: static native void release0(java.io.FileDescriptor, long, long) throws java.io.IOException;
sun/nio/ch/FileKey.class: private native void init(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/FileKey.class: private static native void initIDs();
sun/nio/ch/InheritedChannel.class: private static native int dup(int) throws java.io.IOException;
sun/nio/ch/InheritedChannel.class: private static native int open0(java.lang.String, int) throws java.io.IOException;
sun/nio/ch/InheritedChannel.class: private static native int peerPort0(int);
sun/nio/ch/InheritedChannel.class: private static native int soType0(int);
sun/nio/ch/InheritedChannel.class: private static native java.net.InetAddress peerAddress0(int);
sun/nio/ch/InheritedChannel.class: private static native void close0(int) throws java.io.IOException;
sun/nio/ch/InheritedChannel.class: private static native void dup2(int, int) throws java.io.IOException;
sun/nio/ch/IOUtil.class: static native boolean drain(int) throws java.io.IOException;
sun/nio/ch/IOUtil.class: static native boolean randomBytes(byte[]);
sun/nio/ch/IOUtil.class: static native int fdLimit();
sun/nio/ch/IOUtil.class: static native int fdVal(java.io.FileDescriptor);
sun/nio/ch/IOUtil.class: static native int iovMax();
sun/nio/ch/IOUtil.class: static native long makePipe(boolean);
sun/nio/ch/IOUtil.class: static native void configureBlocking(java.io.FileDescriptor, boolean) throws java.io.IOException;
sun/nio/ch/IOUtil.class: static native void initIDs();
sun/nio/ch/IOUtil.class: static native void setfdVal(java.io.FileDescriptor, int);
sun/nio/ch/KQueue.class: private static native int filterOffset();
sun/nio/ch/KQueue.class: private static native int flagsOffset();
sun/nio/ch/KQueue.class: private static native int identOffset();
sun/nio/ch/KQueue.class: private static native int keventSize();
sun/nio/ch/KQueue.class: static native int keventPoll(int, long, int) throws java.io.IOException;
sun/nio/ch/KQueue.class: static native int keventRegister(int, int, int, int);
sun/nio/ch/KQueue.class: static native int kqueue() throws java.io.IOException;
sun/nio/ch/KQueuePort.class: private static native void close0(int);
sun/nio/ch/KQueuePort.class: private static native void drain1(int) throws java.io.IOException;
sun/nio/ch/KQueuePort.class: private static native void interrupt(int) throws java.io.IOException;
sun/nio/ch/KQueuePort.class: private static native void socketpair(int[]) throws java.io.IOException;
sun/nio/ch/NativeThread.class: static native long current();
sun/nio/ch/NativeThread.class: static native void init();
sun/nio/ch/NativeThread.class: static native void signal(long);
sun/nio/ch/Net.class: private static native boolean canIPv6SocketJoinIPv4Group0();
sun/nio/ch/Net.class: private static native boolean canJoin6WithIPv4Group0();
sun/nio/ch/Net.class: private static native boolean isIPv6Available0();
sun/nio/ch/Net.class: private static native int blockOrUnblock4(boolean, java.io.FileDescriptor, int, int, int) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int connect0(boolean, java.io.FileDescriptor, java.net.InetAddress, int) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int getIntOption0(java.io.FileDescriptor, boolean, int, int) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int isExclusiveBindAvailable();
sun/nio/ch/Net.class: private static native int joinOrDrop4(boolean, java.io.FileDescriptor, int, int, int) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int joinOrDrop6(boolean, java.io.FileDescriptor, byte[], int, byte[]) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int localPort(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int remotePort(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: private static native int socket0(boolean, boolean, boolean, boolean);
sun/nio/ch/Net.class: private static native java.net.InetAddress localInetAddress(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: private static native java.net.InetAddress remoteInetAddress(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: private static native void bind0(java.io.FileDescriptor, boolean, boolean, java.net.InetAddress, int) throws java.io.IOException;
sun/nio/ch/Net.class: private static native void initIDs();
sun/nio/ch/Net.class: private static native void setIntOption0(java.io.FileDescriptor, boolean, int, int, int, boolean) throws java.io.IOException;
sun/nio/ch/Net.class: static native int blockOrUnblock6(boolean, java.io.FileDescriptor, byte[], int, byte[]) throws java.io.IOException;
sun/nio/ch/Net.class: static native int getInterface4(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: static native int getInterface6(java.io.FileDescriptor) throws java.io.IOException;
sun/nio/ch/Net.class: static native void listen(java.io.FileDescriptor, int) throws java.io.IOException;
sun/nio/ch/Net.class: static native void setInterface4(java.io.FileDescriptor, int) throws java.io.IOException;
sun/nio/ch/Net.class: static native void setInterface6(java.io.FileDescriptor, int) throws java.io.IOException;
sun/nio/ch/Net.class: static native void shutdown(java.io.FileDescriptor, int) throws java.io.IOException;
sun/nio/ch/PollArrayWrapper.class: private native int poll0(long, int, long);
sun/nio/ch/PollArrayWrapper.class: private static native void interrupt(int);
sun/nio/ch/SctpChannelImpl.class: private static native int checkConnect(java.io.FileDescriptor, boolean, boolean) throws java.io.IOException;
sun/nio/ch/SctpChannelImpl.class: private static native void initIDs();
sun/nio/ch/SctpChannelImpl.class: static native int receive0(int, sun.nio.ch.SctpResultContainer, long, int, boolean) throws java.io.IOException;
sun/nio/ch/SctpChannelImpl.class: static native int send0(int, long, int, java.net.InetAddress, int, int, int, boolean, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native int branch0(int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native int connect0(int, java.net.InetAddress, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native int getIntOption0(int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native int socket0(boolean) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native java.net.SocketAddress[] getLocalAddresses0(int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native java.net.SocketAddress getPrimAddrOption0(int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native java.net.SocketAddress[] getRemoteAddresses0(int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void bindx(int, java.net.InetAddress[], int, int, boolean, boolean) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void close0(int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void getInitMsgOption0(int, int[]) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void init();
sun/nio/ch/SctpNet.class: static native void listen0(int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void preClose0(int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void setInitMsgOption0(int, int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void setIntOption0(int, int, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void setPeerPrimAddrOption0(int, int, java.net.InetAddress, int, boolean) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void setPrimAddrOption0(int, int, java.net.InetAddress, int) throws java.io.IOException;
sun/nio/ch/SctpNet.class: static native void shutdown0(int, int);
sun/nio/ch/SctpServerChannelImpl.class: private static native int accept0(java.io.FileDescriptor, java.io.FileDescriptor, java.net.InetSocketAddress[]) throws java.io.IOException;
sun/nio/ch/SctpServerChannelImpl.class: private static native void initIDs();
sun/nio/ch/ServerSocketChannelImpl.class: private native int accept0(java.io.FileDescriptor, java.io.FileDescriptor, java.net.InetSocketAddress[]) throws java.io.IOException;
sun/nio/ch/ServerSocketChannelImpl.class: private static native void initIDs();
sun/nio/ch/SocketChannelImpl.class: private static native int checkConnect(java.io.FileDescriptor, boolean, boolean) throws java.io.IOException;
sun/nio/ch/SocketChannelImpl.class: private static native int sendOutOfBandData(java.io.FileDescriptor, byte) throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native boolean port_associate(int, int, long, int) throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native boolean port_dissociate(int, int, long) throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native int port_create() throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native int port_getn(int, long, int, long) throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native void port_close(int);
sun/nio/ch/SolarisEventPort.class: static native void port_get(int, long) throws java.io.IOException;
sun/nio/ch/SolarisEventPort.class: static native void port_send(int, int) throws java.io.IOException;
sun/nio/ch/UnixAsynchronousServerSocketChannelImpl.class: private native int accept0(java.io.FileDescriptor, java.io.FileDescriptor, java.net.InetSocketAddress[]) throws java.io.IOException;
sun/nio/ch/UnixAsynchronousServerSocketChannelImpl.class: private static native void initIDs();
sun/nio/ch/UnixAsynchronousSocketChannelImpl.class: private static native void checkConnect(int) throws java.io.IOException;
sun/nio/fs/GnomeFileTypeDetector.class: private static native boolean initializeGio();
sun/nio/fs/GnomeFileTypeDetector.class: private static native boolean initializeGnomeVfs();
sun/nio/fs/GnomeFileTypeDetector.class: private static native byte[] probeUsingGio(long);
sun/nio/fs/GnomeFileTypeDetector.class: private static native byte[] probeUsingGnomeVfs(long);
sun/nio/fs/LinuxNativeDispatcher.class: private static native int fgetxattr0(int, long, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: private static native long setmntent0(long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: private static native void fremovexattr0(int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: private static native void fsetxattr0(int, long, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: private static native void init();
sun/nio/fs/LinuxNativeDispatcher.class: static native int flistxattr(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: static native int getmntent(long, sun.nio.fs.UnixMountEntry) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxNativeDispatcher.class: static native void endmntent(long) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native int[] eventOffsets();
sun/nio/fs/LinuxWatchService.class: private static native int eventSize();
sun/nio/fs/LinuxWatchService.class: private static native int inotifyAddWatch(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native int inotifyInit() throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native int poll(int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native void configureBlocking(int, boolean) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native void inotifyRmWatch(int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/LinuxWatchService.class: private static native void socketpair(int[]) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisNativeDispatcher.class: private static native void init();
sun/nio/fs/SolarisNativeDispatcher.class: static native int facl(int, int, int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisNativeDispatcher.class: static native int getextmntent(long, sun.nio.fs.UnixMountEntry) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisWatchService.class: private static native int portCreate() throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisWatchService.class: private static native int portGetn(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisWatchService.class: private static native void init();
sun/nio/fs/SolarisWatchService.class: private static native void portAssociate(int, int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisWatchService.class: private static native void portDissociate(int, int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/SolarisWatchService.class: private static native void portSend(int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixCopyFile.class: static native void transfer(int, int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native byte[] readlink0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native byte[] realpath0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native int getgrnam0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native int getpwnam0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native int init();
sun/nio/fs/UnixNativeDispatcher.class: private static native int open0(long, int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native int openat0(int, long, int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native long fopen0(long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native long opendir0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native long pathconf0(long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void access0(long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void chmod0(long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void chown0(long, int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void fstatat0(int, long, int, sun.nio.fs.UnixFileAttributes) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void lchown0(long, int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void link0(long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void lstat0(long, sun.nio.fs.UnixFileAttributes) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void mkdir0(long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void mknod0(long, int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void rename0(long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void renameat0(int, long, int, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void rmdir0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void stat0(long, sun.nio.fs.UnixFileAttributes) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void statvfs0(long, sun.nio.fs.UnixFileStoreAttributes) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void symlink0(long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void unlink0(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void unlinkat0(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: private static native void utimes0(long, long, long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native byte[] getcwd();
sun/nio/fs/UnixNativeDispatcher.class: static native byte[] getgrgid(int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native byte[] getpwuid(int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native byte[] readdir(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native byte[] strerror(int);
sun/nio/fs/UnixNativeDispatcher.class: static native int dup(int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native int read(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native int write(int, long, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native long fdopendir(int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native long fpathconf(int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void closedir(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void close(int);
sun/nio/fs/UnixNativeDispatcher.class: static native void fchmod(int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void fchown(int, int, int) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void fclose(long) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void fstat(int, sun.nio.fs.UnixFileAttributes) throws sun.nio.fs.UnixException;
sun/nio/fs/UnixNativeDispatcher.class: static native void futimes(int, long, long, sun.nio.fs.UnixPath) throws sun.nio.fs.UnixException;
sun/print/CUPSPrinter.class: private static native boolean canConnect(java.lang.String, int);
sun/print/CUPSPrinter.class: private static native boolean initIDs();
sun/print/CUPSPrinter.class: private static native int getCupsPort();
sun/print/CUPSPrinter.class: private static native java.lang.String getCupsServer();
sun/print/CUPSPrinter.class: private static synchronized native float[] getPageSizes(java.lang.String);
sun/print/CUPSPrinter.class: private static synchronized native java.lang.String[] getMedia(java.lang.String);
sun/reflect/ConstantPool.class: private native double getDoubleAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native float getFloatAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native int getIntAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native int getSize0(java.lang.Object);
sun/reflect/ConstantPool.class: private native java.lang.Class getClassAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.Class getClassAtIfLoaded0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.reflect.Field getFieldAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.reflect.Field getFieldAtIfLoaded0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.reflect.Member getMethodAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.reflect.Member getMethodAtIfLoaded0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.String[] getMemberRefInfoAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.String getStringAt0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native java.lang.String getUTF8At0(java.lang.Object, int);
sun/reflect/ConstantPool.class: private native long getLongAt0(java.lang.Object, int);
sun/reflect/NativeConstructorAccessorImpl.class: private static native java.lang.Object newInstance0(java.lang.reflect.Constructor, java.lang.Object[]) throws java.lang.InstantiationException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;
sun/reflect/NativeMethodAccessorImpl.class: private static native java.lang.Object invoke0(java.lang.reflect.Method, java.lang.Object, java.lang.Object[]);
sun/reflect/Reflection.class: private static native int getClassAccessFlags(java.lang.Class);
sun/reflect/Reflection.class: private static native java.lang.Class getCallerClass0(int);
sun/reflect/Reflection.class: public static native java.lang.Class getCallerClass();
sun/security/jgss/wrapper/GSSLibStub.class: native boolean compareName(long, long);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] acceptContext(long, org.ietf.jgss.ChannelBinding, byte[], sun.security.jgss.wrapper.NativeGSSContext);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] exportContext(long);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] exportName(long) throws org.ietf.jgss.GSSException;
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] getMic(long, int, byte[]);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] initContext(long, long, org.ietf.jgss.ChannelBinding, byte[], sun.security.jgss.wrapper.NativeGSSContext);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] unwrap(long, byte[], org.ietf.jgss.MessageProp);
sun/security/jgss/wrapper/GSSLibStub.class: native byte[] wrap(long, byte[], org.ietf.jgss.MessageProp);
sun/security/jgss/wrapper/GSSLibStub.class: native int getContextTime(long);
sun/security/jgss/wrapper/GSSLibStub.class: native int getCredTime(long);
sun/security/jgss/wrapper/GSSLibStub.class: native int getCredUsage(long);
sun/security/jgss/wrapper/GSSLibStub.class: native int wrapSizeLimit(long, int, int, int);
sun/security/jgss/wrapper/GSSLibStub.class: native java.lang.Object[] displayName(long) throws org.ietf.jgss.GSSException;
sun/security/jgss/wrapper/GSSLibStub.class: native long acquireCred(long, int, int) throws org.ietf.jgss.GSSException;
sun/security/jgss/wrapper/GSSLibStub.class: native long canonicalizeName(long);
sun/security/jgss/wrapper/GSSLibStub.class: native long deleteContext(long);
sun/security/jgss/wrapper/GSSLibStub.class: native long getContextName(long, boolean);
sun/security/jgss/wrapper/GSSLibStub.class: native long getCredName(long);
sun/security/jgss/wrapper/GSSLibStub.class: native long importName(byte[], org.ietf.jgss.Oid);
sun/security/jgss/wrapper/GSSLibStub.class: native long[] inquireContext(long);
sun/security/jgss/wrapper/GSSLibStub.class: native long releaseCred(long);
sun/security/jgss/wrapper/GSSLibStub.class: native org.ietf.jgss.Oid getContextMech(long);
sun/security/jgss/wrapper/GSSLibStub.class: native org.ietf.jgss.Oid[] inquireNamesForMech() throws org.ietf.jgss.GSSException;
sun/security/jgss/wrapper/GSSLibStub.class: native sun.security.jgss.wrapper.NativeGSSContext importContext(byte[]);
sun/security/jgss/wrapper/GSSLibStub.class: native void releaseName(long);
sun/security/jgss/wrapper/GSSLibStub.class: native void verifyMic(long, byte[], byte[], org.ietf.jgss.MessageProp);
sun/security/jgss/wrapper/GSSLibStub.class: private static native long getMechPtr(byte[]);
sun/security/jgss/wrapper/GSSLibStub.class: static native boolean init(java.lang.String);
sun/security/jgss/wrapper/GSSLibStub.class: static native org.ietf.jgss.Oid[] indicateMechs();
sun/security/krb5/Config.class: private static native java.lang.String getWindowsDirectory(boolean);
sun/security/krb5/Credentials.class: private static native sun.security.krb5.Credentials acquireDefaultNativeCreds(int[]);
sun/security/krb5/internal/ccache/FileCredentialsCache.class: private static native java.lang.String nativeGetDefaultCacheName() throws java.lang.Exception;
sun/security/krb5/SCDynamicStoreConfig.class: private static native java.util.Hashtable<java.lang.String, java.lang.Object> getKerberosConfig();
sun/security/krb5/SCDynamicStoreConfig.class: private static native void installNotificationCallback();
sun/security/smartcardio/PCSC.class: static native byte[] SCardControl(long, int, byte[]) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native byte[] SCardStatus(long, byte[]) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native byte[] SCardTransmit(long, int, byte[], int, int) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native int[] SCardGetStatusChange(long, long, int[], java.lang.String[]) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native java.lang.String[] SCardListReaders(long) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native long SCardConnect(long, java.lang.String, int, int) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native long SCardEstablishContext(int) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native void SCardBeginTransaction(long) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native void SCardDisconnect(long, int) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PCSC.class: static native void SCardEndTransaction(long, int) throws sun.security.smartcardio.PCSCException;
sun/security/smartcardio/PlatformPCSC.class: private static native void initialize(java.lang.String);
sun/tracing/dtrace/JVM.class: private static native boolean isEnabled0(java.lang.reflect.Method);
sun/tracing/dtrace/JVM.class: private static native boolean isSupported0();
sun/tracing/dtrace/JVM.class: private static native java.lang.Class<?> defineClass0(java.lang.ClassLoader, java.lang.String, byte[], int, int);
sun/tracing/dtrace/JVM.class: private static native long activate0(java.lang.String, sun.tracing.dtrace.DTraceProvider[]);
sun/tracing/dtrace/JVM.class: private static native void dispose0(long);
*/
