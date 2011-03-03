package com.nirima.stapler;

import hudson.model.Hudson;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jvnet.tiger_types.Lister;
import org.kohsuke.stapler.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 22/12/2010
 * Time: 16:52
 * To change this template use File | Settings | File Templates.
 */
public class FakeStapler implements StaplerRequest {


    /**
     * Cached result of {@link #getSubmittedForm()}
     */
    private JSONObject structuredForm;

    private Map<String, Object> data;

    /**
     * If the request is "multipart/form-data", parsed result goes here.
     *
     * @see #parseMultipartFormData()
     */
    private Map<String, FileItem> parsedFormData;
    private ClassLoader classLoader;

    public FakeStapler(Map<String, Object> data, ClassLoader classLoader) {
        this.data = data;
        this.classLoader = classLoader;
    }

    public boolean isJavaScriptProxyCall() {
        String ct = getContentType();
        return ct!=null && ct.startsWith("application/x-stapler-method-invocation");
    }

    public Stapler getStapler() {
        return null;
    }

    public String getRestOfPath() {
        return "";
    }

    public String getOriginalRestOfPath() {
        return "";
    }

    public ServletContext getServletContext() {
        return null;
    }

    public RequestDispatcher getView(Object it,String viewName) throws IOException {
        return getView(it.getClass(),it,viewName);
    }

    public RequestDispatcher getView(Class clazz, String viewName) throws IOException {
        return getView(clazz,null,viewName);
    }

    public RequestDispatcher getView(Class clazz, Object it, String viewName) throws IOException {
        return null;
    }

    public String getRootPath() {
       return "";
    }

    public String getReferer() {
        return getHeader("Referer");
    }

    public List<Ancestor> getAncestors() {
        return Collections.EMPTY_LIST;
    }

    public Ancestor findAncestor(Class type) {


        return null;
    }

    public <T> T findAncestorObject(Class<T> type) {
        Ancestor a = findAncestor(type);
        if(a==null) return null;
        return type.cast(a.getObject());
    }

    public Ancestor findAncestor(Object anc) {


        return null;
    }

    public boolean hasParameter(String name) {
        return getParameter(name)!=null;
    }

    public String getOriginalRequestURI() {
        return "";
    }

    public boolean checkIfModified(long lastModified, StaplerResponse rsp) {
        return checkIfModified(lastModified,rsp,0);
    }

    public boolean checkIfModified(long lastModified, StaplerResponse rsp, long expiration) {

        return false;
    }

    public boolean checkIfModified(Date timestampOfResource, StaplerResponse rsp) {
        return checkIfModified(timestampOfResource.getTime(),rsp);
    }

    public boolean checkIfModified(Calendar timestampOfResource, StaplerResponse rsp) {
        return checkIfModified(timestampOfResource.getTimeInMillis(),rsp);
    }

    public void bindParameters(Object bean) {
        bindParameters(bean,"");
    }

    public void bindParameters(Object bean, String prefix) {
        Enumeration e = getParameterNames();
        while(e.hasMoreElements()) {
            String name = (String)e.nextElement();

            fill(bean, name, getParameter(name) );
        }
    }

    public <T>
    List<T> bindParametersToList(Class<T> type, String prefix) {
        List<T> r = new ArrayList<T>();

        int len = Integer.MAX_VALUE;

        Enumeration e = getParameterNames();
        while(e.hasMoreElements()) {
            String name = (String)e.nextElement();
            if(name.startsWith(prefix))
                len = Math.min(len,getParameterValues(name).length);
        }

        if(len==Integer.MAX_VALUE)
            return r;   // nothing

        try {
            loadConstructorParamNames(type);
            // use the designated constructor for databinding
            for( int i=0; i<len; i++ )
                r.add(bindParameters(type,prefix,i));
        } catch (NoStaplerConstructorException _) {
            // no designated data binding constructor. use reflection
            try {
                for( int i=0; i<len; i++ ) {
                    T t = type.newInstance();
                    r.add(t);

                    e = getParameterNames();
                    while(e.hasMoreElements()) {
                        String name = (String)e.nextElement();
                        if(name.startsWith(prefix))
                            fill(t, name.substring(prefix.length()), getParameterValues(name)[i] );
                    }
                }
            } catch (InstantiationException x) {
                throw new InstantiationError(x.getMessage());
            } catch (IllegalAccessException x) {
                throw new IllegalAccessError(x.getMessage());
            }
        }

        return r;
    }

    public <T> T bindParameters(Class<T> type, String prefix) {
        return bindParameters(type,prefix,0);
    }

    public <T> T bindParameters(Class<T> type, String prefix, int index) {
        String[] names = loadConstructorParamNames(type);

        // the actual arguments to invoke the constructor with.
        Object[] args = new Object[names.length];

        // constructor
        Constructor<T> c = findConstructor(type, names.length);
        Class[] types = c.getParameterTypes();

        // convert parameters
        for( int i=0; i<names.length; i++ ) {
            String[] values = getParameterValues(prefix + names[i]);
            String param;
            if(values!=null)
                param = values[index];
            else
                param = null;

            Converter converter = Stapler.lookupConverter(types[i]);
            if (converter==null)
                throw new IllegalArgumentException("Unable to convert to "+types[i]);

            args[i] = converter.convert(types[i],param);
        }

        return invokeConstructor(c, args);
    }

    public <T> T bindJSON(Class<T> type, JSONObject src) {
        try {
            if(src.has("stapler-class")) {
                // sub-type is specified in JSON.
                // note that this can come from malicious clients, so we need to make sure we don't have security issues.

                ClassLoader cl = getClassLoader();
                String className = src.getString("stapler-class");
                try {
                    Class<?> subType = cl.loadClass(className);
                    if(!type.isAssignableFrom(subType))
                        throw new IllegalArgumentException("Specified type "+subType+" is not assignable to the expected "+type);
                    type = (Class)subType; // I'm being lazy here
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Class "+className+" is specified in JSON, but no such class found in "+cl,e);
                }
            }

            if (type==JSONObject.class || type==JSON.class) return type.cast(src);

            String[] names = loadConstructorParamNames(type);

            // the actual arguments to invoke the constructor with.
            Object[] args = new Object[names.length];

            // constructor
            Constructor<T> c = findConstructor(type, names.length);
            Class[] types = c.getParameterTypes();
            Type[] genTypes = c.getGenericParameterTypes();

            // convert parameters
            for( int i=0; i<names.length; i++ ) {
                try {
                    args[i] = bindJSON(genTypes[i],types[i],src.get(names[i]));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Failed to convert the "+names[i]+" parameter of the constructor "+c,e);
                }
            }

            return invokeConstructor(c, args);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to instantiate "+type+" from "+src,e);
        }
    }

    public Object bindJSON(Type type, Class erasure, Object json) {
        return new TypePair(type,erasure).convertJSON(json);
    }

    public void bindJSON(Object bean, JSONObject src) {
        try {
            for( String key : (Set<String>)src.keySet() ) {
                TypePair type = getPropertyType(bean, key);
                if(type==null)
                    continue;

                fill(bean,key, type.convertJSON(src.get(key)));
            }
        } catch (IllegalAccessException e) {
            IllegalAccessError x = new IllegalAccessError(e.getMessage());
            x.initCause(e);
            throw x;
        } catch (InvocationTargetException x) {
            Throwable e = x.getTargetException();
            if(e instanceof RuntimeException)
                throw (RuntimeException)e;
            if(e instanceof Error)
                throw (Error)e;
            throw new RuntimeException(x);
        }
    }

    public <T> List<T> bindJSONToList(Class<T> type, Object src) {
        ArrayList<T> r = new ArrayList<T>();
        if (src instanceof JSONObject) {
            JSONObject j = (JSONObject) src;
            r.add(bindJSON(type,j));
        }
        if (src instanceof JSONArray) {
            JSONArray a = (JSONArray) src;
            for (Object o : a) {
                if (o instanceof JSONObject) {
                    JSONObject j = (JSONObject) o;
                    r.add(bindJSON(type,j));
                }
            }
        }
        return r;
    }


    private <T> T invokeConstructor(Constructor<T> c, Object[] args) {
        try {
            return c.newInstance(args);
        } catch (InstantiationException e) {
            InstantiationError x = new InstantiationError(e.getMessage());
            x.initCause(e);
            throw x;
        } catch (IllegalAccessException e) {
            IllegalAccessError x = new IllegalAccessError(e.getMessage());
            x.initCause(e);
            throw x;
        } catch (InvocationTargetException e) {
            Throwable x = e.getTargetException();
            if(x instanceof Error)
                throw (Error)x;
            if(x instanceof RuntimeException)
                throw (RuntimeException)x;
            throw new IllegalArgumentException(x);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to invoke "+c+" with "+ Arrays.asList(args),e);
        }
    }

    private <T> Constructor<T> findConstructor(Class<T> type, int length) {
        Constructor<?>[] ctrs = type.getConstructors();
        // one with DataBoundConstructor is the most reliable
        for (Constructor c : ctrs) {
            if(c.getAnnotation(DataBoundConstructor.class)!=null) {
                if(c.getParameterTypes().length!=length)
                    throw new IllegalArgumentException(c+" has @DataBoundConstructor but it doesn't match with your .stapler file. Try clean rebuild");
                return c;
            }
        }
        // if not, maybe this was from @stapler-constructor,
        // so look for the constructor with the expected argument length.
        // this is not very reliable.
        for (Constructor c : ctrs) {
            if(c.getParameterTypes().length==length)
                return c;
        }
        throw new IllegalArgumentException(type+" does not have a constructor with "+length+" arguments");
    }

    /**
     * Determines the constructor parameter names.
     *
     * <p>
     * First, try to load names from the debug information. Otherwise
     * if there's the .stapler file, load it as a property file and determines the constructor parameter names.
     * Otherwise, look for {@link CapturedParameterNames} annotation.
     */
    private String[] loadConstructorParamNames(Class<?> type) {
        Constructor<?>[] ctrs = type.getConstructors();
        // which constructor was data bound?
        Constructor<?> dbc = null;
        for (Constructor<?> c : ctrs) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) {
                dbc = c;
                break;
            }
        }

        if (dbc==null)
            throw new NoStaplerConstructorException("There's no @DataBoundConstructor on any constructor of " + type);

        String[] names = ClassDescriptor.loadParameterNames(dbc);
        if (names.length==dbc.getParameterTypes().length)
            return names;

        String resourceName = type.getName().replace('.', '/').replace('$','/') + ".stapler";
        ClassLoader cl = type.getClassLoader();
        if(cl==null)
            throw new NoStaplerConstructorException(type+" is a built-in type");
        InputStream s = cl.getResourceAsStream(resourceName);
        if (s != null) {// load the property file and figure out parameter names
            try {
                Properties p = new Properties();
                p.load(s);
                s.close();

                String v = p.getProperty("constructor");
                if (v.length() == 0) return new String[0];
                return v.split(",");
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to load " + resourceName, e);
            }
        }

        // no debug info and no stapler file
        throw new NoStaplerConstructorException(
                "Unable to find " + resourceName + ". " +
                        "Run 'mvn clean compile' once to run the annotation processor.");
    }

    private static void fill(Object bean, String key, Object value) {
        StringTokenizer tokens = new StringTokenizer(key);
        while(tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            boolean last = !tokens.hasMoreTokens();  // is this the last token?

            try {
                if(last) {
                    copyProperty(bean,token,value);
                } else {
                    bean = BeanUtils.getProperty(bean, token);
                }
            } catch (IllegalAccessException x) {
                throw new IllegalAccessError(x.getMessage());
            } catch (InvocationTargetException x) {
                Throwable e = x.getTargetException();
                if(e instanceof RuntimeException)
                    throw (RuntimeException)e;
                if(e instanceof Error)
                    throw (Error)e;
                throw new RuntimeException(x);
            } catch (NoSuchMethodException e) {
                // ignore if there's no such property
            }
        }
    }

    public String getAuthType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Cookie[] getCookies() {
        return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getDateHeader(String name) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getHeader(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getHeaders(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getHeaderNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getIntHeader(String name) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getMethod() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPathInfo() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPathTranslated() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getContextPath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getQueryString() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRemoteUser() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isUserInRole(String role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Principal getUserPrincipal() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRequestedSessionId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRequestURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public StringBuffer getRequestURL() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getServletPath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public HttpSession getSession(boolean create) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public HttpSession getSession() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRequestedSessionIdValid() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getAttribute(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getAttributeNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getCharacterEncoding() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getContentLength() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getContentType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServletInputStream getInputStream() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getParameter(String name) {
        if( !data.containsKey(name) )
            return null;

        return "" + data.get(name);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getParameterNames() {
        Vector v = new Vector(data.keySet());
        return v.elements();
    }

    public String[] getParameterValues(String name) {
        // TODO : Don't know about multiples here.
        if( !data.containsKey(name) )
            return new String[0];

        return new String[] { (String)data.get(name) };
    }

    public Map getParameterMap() {
        return data;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getProtocol() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getScheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getServerName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getServerPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BufferedReader getReader() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRemoteAddr() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRemoteHost() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAttribute(String name, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeAttribute(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Locale getLocale() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getLocales() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSecure() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRealPath(String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getRemotePort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLocalName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLocalAddr() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLocalPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Information about the type.
     */
    private final class TypePair {
        final Type genericType;
        final Class type;

        TypePair(Type genericType, Class type) {
            this.genericType = genericType;
            this.type = type;
        }

        TypePair(Field f) {
            this(f.getGenericType(),f.getType());
        }

        /**
         * Converts the given JSON object (either {@link JSONObject}, {@link JSONArray}, or other primitive types
         * in JSON, to the type represented by the 'this' object.
         */
        public Object convertJSON(Object o) {
            if(o==null) {
                // this method returns null if the type is not primitive, which works.
                return ReflectionUtils.getVmDefaultValueFor(type);
            }

            if (type==JSONArray.class) {
                if (o instanceof JSONArray) return o;

                JSONArray a = new JSONArray();
                a.add(o);
                return a;
            }

            Lister l = Lister.create(type,genericType);

            if (o instanceof JSONObject) {
                JSONObject j = (JSONObject) o;

                if (j.isNullObject())   // another flavor of null. json-lib sucks.
                    return ReflectionUtils.getVmDefaultValueFor(type);

                if(l==null) {
                    // single value conversion
                    return bindJSON(type,j);
                } else {
                    if(j.has("stapler-class-bag")) {
                        // this object is a hash from class names to their parameters
                        // build them into a collection via Lister

//                        ClassLoader cl = stapler.getWebApp().getClassLoader();
//                        for (Map.Entry<String,Object> e : (Set<Map.Entry<String,Object>>)j.entrySet()) {
//                            Object v = e.getValue();
//
//                            String className = e.getKey().replace('-','.'); // decode JSON-safe class name escaping
//                            try {
//                                Class<?> itemType = cl.loadClass(className);
//                                if (v instanceof JSONObject) {
//                                    l.add(bindJSON(itemType, (JSONObject) v));
//                                }
//                                if (v instanceof JSONArray) {
//                                    for(Object i : bindJSONToList(itemType, (JSONArray) v))
//                                        l.add(i);
//                                }
//                            } catch (ClassNotFoundException e1) {
//                                // ignore unrecognized element
//                            }
//                        }
                    } else if (Enum.class.isAssignableFrom(l.itemType)) {
                        // this is a hash of element names as enum constant names
                        for (Map.Entry<String,Object> e : (Set<Map.Entry<String,Object>>)j.entrySet()) {
                            Object v = e.getValue();
                            if (v==null || (v instanceof Boolean && !(Boolean)v))
                                continue;       // skip if the value is null or false

                            l.add(Enum.valueOf(l.itemType,e.getKey()));
                        }
                    } else {
                        // only one value given to the collection
                        l.add(new TypePair(l.itemGenericType,l.itemType).convertJSON(j));
                    }
                    return l.toCollection();
                }
            }
            if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                TypePair itemType = new TypePair(l.itemGenericType,l.itemType);
                for (Object item : a)
                    l.add(itemType.convertJSON(item));
                return l.toCollection();
            }

            if(Enum.class.isAssignableFrom(type))
                return Enum.valueOf(type,o.toString());

            if (l==null) {// single value conversion
                Converter converter = Stapler.lookupConverter(type);
                if (converter==null)
                    throw new IllegalArgumentException("Unable to convert to "+type);

                return converter.convert(type,o);
            } else {// single value in a collection
                Converter converter = Stapler.lookupConverter(l.itemType);
                if (converter==null)
                    throw new IllegalArgumentException("Unable to convert to "+type);

                l.add(converter.convert(type,o));
                return l.toCollection();
            }
        }
    }

    /**
     * Gets the type of the field/property designate by the given name.
     */
    private TypePair getPropertyType(Object bean, String name) throws IllegalAccessException, InvocationTargetException {
        try {
            PropertyDescriptor propDescriptor = PropertyUtils.getPropertyDescriptor(bean, name);
            if(propDescriptor!=null) {
                Method m = propDescriptor.getWriteMethod();
                if(m!=null)
                    return new TypePair(m.getGenericParameterTypes()[0], m.getParameterTypes()[0]);
            }
        } catch (NoSuchMethodException e) {
            // no such property
        }

        // try a field
        try {
            return new TypePair(bean.getClass().getField(name));
        } catch (NoSuchFieldException e) {
            // no such field
        }

        return null;
    }

    /**
     * Sets the property/field value of the given name, by performing a value type conversion if necessary.
     */
    private static void copyProperty(Object bean, String name, Object value) throws IllegalAccessException, InvocationTargetException {
        PropertyDescriptor propDescriptor;
        try {
            propDescriptor =
                PropertyUtils.getPropertyDescriptor(bean, name);
        } catch (NoSuchMethodException e) {
            propDescriptor = null;
        }
        if ((propDescriptor != null) &&
            (propDescriptor.getWriteMethod() == null)) {
            propDescriptor = null;
        }
        if (propDescriptor != null) {
            Converter converter = Stapler.lookupConverter(propDescriptor.getPropertyType());
            if (converter != null)
                value = converter.convert(propDescriptor.getPropertyType(), value);
            try {
                PropertyUtils.setSimpleProperty(bean, name, value);
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
            return;
        }

        // try a field
        try {
            Field field = bean.getClass().getField(name);
            Converter converter = ConvertUtils.lookup(field.getType());
            if (converter != null)
                value = converter.convert(field.getType(), value);
            field.set(bean,value);
        } catch (NoSuchFieldException e) {
            // no such field
        }
    }

    private void parseMultipartFormData() throws ServletException {
        if(parsedFormData!=null)    return;

        parsedFormData = new HashMap<String,FileItem>();
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        try {
            for( FileItem fi : (List<FileItem>)upload.parseRequest(this) )
                parsedFormData.put(fi.getFieldName(),fi);
        } catch (FileUploadException e) {
            throw new ServletException(e);
        }
    }

    public JSONObject getSubmittedForm() throws ServletException {
        if(structuredForm==null) {
            String ct = getContentType();
            String p = null;
            boolean isSubmission; // for error diagnosis, if something is submitted, set to true

            if(ct!=null && ct.startsWith("multipart/")) {
                isSubmission=true;
                parseMultipartFormData();
                FileItem item = parsedFormData.get("json");
                if(item!=null)
                    p = item.getString();
            } else {
                p = getParameter("json");
                isSubmission = !getParameterMap().isEmpty();
            }

            if(p==null) {
                // no data submitted
                try {
                    StaplerResponse rsp = Stapler.getCurrentResponse();
                    if(isSubmission)
                        rsp.sendError(SC_BAD_REQUEST,"This page expects a form submission");
                    else
                        rsp.sendError(SC_BAD_REQUEST,"Nothing is submitted");
                    rsp.getWriter().close();
                    throw new Error("This page expects a form submission");
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
            structuredForm = JSONObject.fromObject(p);
        }
        return structuredForm;
    }

    public FileItem getFileItem(String name) throws ServletException, IOException {
        parseMultipartFormData();
        if(parsedFormData==null)    return null;
        FileItem item = parsedFormData.get(name);
        if(item==null || item.isFormField())    return null;
        return item;
    }
}