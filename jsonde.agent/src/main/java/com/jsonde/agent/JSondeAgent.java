package com.jsonde.agent;

import com.jsonde.api.Message;
import com.jsonde.api.MessageListener;
import com.jsonde.api.configuration.AgentConfigurationMessage;
import com.jsonde.api.configuration.ClassFilterDto;
import com.jsonde.profiler.Profiler;
import com.jsonde.profiler.network.NetworkServerException;
import com.jsonde.util.ClassUtils;
import com.jsonde.util.StringUtils;
import com.jsonde.util.io.IO;
import com.jsonde.util.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * Commenti Javadoc
 * @author gabriele
 *
 */
public class JSondeAgent implements MessageListener, ClassFileTransformer {

    private final static Log log = Log.getLog(JSondeAgent.class);

    private static final int DEFAULT_PORT_NUMBER = 60001;

    private final ClassFileTransformer byteCodeTransformer;
    private final Profiler profiler;

    private final String arguments;
    private final Instrumentation instrumentation;

    private final ClassLoader resolveAgentLibrariesClassLoader;

    public static void premain(final String arg, Instrumentation instr) {
        JSondeAgent jSondeAgent = new JSondeAgent(arg, instr);
        jSondeAgent.execute();
        jSondeAgent.setTransformer();
    }

    @SuppressWarnings("unused")
    public static void agentmain(String arg, final Instrumentation instr) {
        final JSondeAgent jSondeAgent = new JSondeAgent(arg, instr);
        new Thread(new Runnable() {

            public void run() {
                jSondeAgent.execute();
                jSondeAgent.redefineLoadedClasses();
                jSondeAgent.setTransformer();
                /*try {
                    if (instrumentation.isRetransformClassesSupported()) {
                        jSondeAgent.setTransformer();
                        Class[] classes = instrumentation.getAllLoadedClasses();

                        List<Class> modifiableClasses = new ArrayList<Class>(classes.length / 2);

                        for (Class clazz : classes) {
                            if (instrumentation.isModifiableClass(clazz)) {
                                modifiableClasses.add(clazz);
                            }
                        }

                        int classesCount = modifiableClasses.size();
                        int chunkSize = 100;
                        int chunkCount = classesCount / chunkSize;

                        for (int i = 0; i < chunkCount; i++) {

                            Class[] classesChunk =
                                    modifiableClasses.
                                            subList(i * chunkSize, (i+1) * chunkSize).
                                            toArray(new Class[chunkSize]);

                            instrumentation.retransformClasses(classesChunk);

                        }

                        Class[] classesChunk =
                                    modifiableClasses.
                                            subList(classesCount - (classesCount % chunkSize), classesCount).
                                            toArray(new Class[chunkSize]);

                        instrumentation.retransformClasses(classesChunk);

                    } else {
                        jSondeAgent.redefineLoadedClasses();
                        jSondeAgent.setTransformer();
                    }
                } catch (NoSuchMethodError e) {
                    jSondeAgent.redefineLoadedClasses();
                    jSondeAgent.setTransformer();
                } catch (UnmodifiableClassException e) {
                   
                }*/

            }

        }).start();
    }

    public JSondeAgent(String arguments, Instrumentation instrumentation) {

        System.out.println("jSonde agent started");

        this.arguments = arguments;
        this.instrumentation = instrumentation;

        ResolveAgentLibrariesClassLoader resolveAgentLibrariesClassLoader = new ResolveAgentLibrariesClassLoader();

        this.resolveAgentLibrariesClassLoader = resolveAgentLibrariesClassLoader;

        byteCodeTransformer = createByteCodeTransformer();

        int portNumber = getPortNumber();

        profiler = Profiler.initializeProfiler(instrumentation, portNumber);
    }

    private int getPortNumber() {
        try {
            return Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT_NUMBER;
        }
    }

    private ClassFileTransformer createByteCodeTransformer() {
        try {
            return (ClassFileTransformer)
                    resolveAgentLibrariesClassLoader.
                            loadClass("com.jsonde.instrumentation.ByteCodeTransformer").
                            newInstance();
        } catch (InstantiationException e) {
        	JOptionPane.showMessageDialog (
					null , "Eccezione lanciata"
			);
        } catch (IllegalAccessException e) {
        	JOptionPane.showMessageDialog (
					null , "Eccezione lanciata"
			);
        } catch (ClassNotFoundException e) {
        	JOptionPane.showMessageDialog (
					null , "Eccezione lanciata"
			);
        }
        return null;
    }

    public void execute() {

        final String METHOD_NAME = "execute()";

        try {
            startServer();
        } catch (NetworkServerException e) {
            log.error(METHOD_NAME, e);
        }

    }

    public void setTransformer() {
        instrumentation.addTransformer(this);
    }

    private void redefineLoadedClasses() {
      
            try {
            	for (Class clazz : instrumentation.getAllLoadedClasses()) {
            		redefineLoadedClass(clazz);
            	}
            } catch (Exception e) {
                System.out.println("Error while transforming class " + clazz);
            }

      
    }

    private void redefineLoadedClass(Class clazz) {

        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }

        String className = clazz.getName();

        if (shouldTransformClass(className)&&) {
        	URL classFileResourceURL;
            ClassLoader classLoader = clazz.getClassLoader();
            classFileResourceURL = ClassLoader.getSystemResource(
                    ClassUtils.convertClassNameToResourceName(className));
            if (null == classLoader) {
                classFileResourceURL = ClassLoader.getSystemResource(
                        ClassUtils.convertClassNameToResourceName(className));
            } else if (null == classLoader) {
            	URL classFileResourceURL;
                ClassLoader classLoader = clazz.getClassLoader();
                classFileResourceURL = classLoader.getResource(
                        ClassUtils.convertClassNameToResourceName(className));
            } else if (null == classFileResourceURL)
                return;

            InputStream byteCodeInputStream = null;
            ByteArrayOutputStream originalByteArrayOutputStream = new ByteArrayOutputStream();

            try {

                byteCodeInputStream = classFileResourceURL.openStream();

                int a = byteCodeInputStream.available();
                while (a > 0) {
                    originalByteArrayOutputStream.write(byteCodeInputStream.read());
                    a = byteCodeInputStream.available();
                }

                byte[] bytecode = originalByteArrayOutputStream.toByteArray();

                bytecode = transform(
                        classLoader, className, clazz, clazz.getProtectionDomain(), bytecode
                );

                if (null != bytecode) {
                    Profiler.getProfiler().redefineClass(
                            bytecode,
                            className,
                            clazz.getClassLoader());
                }

            } catch (IOException e) {
            	JOptionPane.showMessageDialog (
    					null , "Eccezione lanciata"
    			);
            } catch (IllegalClassFormatException e) {
            	JOptionPane.showMessageDialog (
    					null , "Eccezione lanciata"
    			);
            } finally {
                IO.close(originalByteArrayOutputStream);
                IO.close(byteCodeInputStream);
            }

        }

    }

    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {

        if (null != agentConfigurationMessage && null != agentConfigurationMessage.getClassFilters()&&!shouldTransformClass(className)) {
                return classfileBuffer;
        }

        Thread currentThread = Thread.currentThread();

        for (Long profilerThreadId : Profiler.getProfiler().getProfilerThreadIds()) {
            if (currentThread.getId() == profilerThreadId) {
                return classfileBuffer;
            }
        }

        ClassLoader contextClassLoader = currentThread.getContextClassLoader();

        try {
            currentThread.setContextClassLoader(resolveAgentLibrariesClassLoader);

            byte[] transformedBytes = byteCodeTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

            if ((null == loader || null == loader.getParent())&&!name.startsWith("com.jsonde")) {

                String name = ClassUtils.getFullyQualifiedName(className);

                
                Profiler.getProfiler().redefineClass(
                		transformedBytes,name,loader
                );
                return classfileBuffer;

            } else {
                return transformedBytes;
            }

        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }

    }

    private boolean shouldTransformClass(String className) {

        if ((className.startsWith("com.jsonde")) && (!className.startsWith("com.jsonde.instrumentation.samples")))
            return false;

        boolean transform = true;

        for (ClassFilterDto classFilter : agentConfigurationMessage.getClassFilters()) {
            String regex = StringUtils.wildcardToRegex(classFilter.getPackageName());
            boolean matches = ClassUtils.getFullyQualifiedName(className).matches(regex);

            if (matches) {
                transform = classFilter.isInclusive();
            }

        }
        return transform;
    }

    public void startServer() throws NetworkServerException {

    	synchronized (this){
    		final String METHOD_NAME = "startServer()";

            profiler.addMessageListener(this);
            profiler.start();
            try {
            	while (null == agentConfigurationMessage) {
                    wait();
                }
            } catch (InterruptedException e) {
                    log.error(METHOD_NAME, e);
                    Thread.currentThread().interrupt();    
            }

            profiler.removeMessageListener(this);

    	}
    }

    private volatile AgentConfigurationMessage agentConfigurationMessage;

    public void onMessage(Message message) {

    	synchronized (this){
    		final String METHOD_NAME = "onMessage(Message)";

            if (log.isTraceEnabled()) {
                log.trace(METHOD_NAME, "Recieved Message" + message);
            }

            if (message instanceof AgentConfigurationMessage) {
                agentConfigurationMessage = (AgentConfigurationMessage) message;
                notifyAll();
            }

    	}
    }

}
