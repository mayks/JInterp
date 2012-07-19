import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class jInterp 
{
	public static boolean detailprint = false;
	public static void main(String args[]) throws IOException, 
												ClassNotFoundException, 
												InstantiationException, 
												IllegalAccessException, 
												SecurityException, 
												NoSuchMethodException, 
												IllegalArgumentException, 
												InvocationTargetException
	{
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].compareTo("-d") == 0)
			{
				detailprint = true;
			}
		}
		
		int i = 0;
		String strName = "Interp_";
		String currClassName = strName+i;
		String prevClassName = null;
		
		System.out.print("> ");
		Scanner scan = new Scanner(System.in);

		PrintStream printErr = new PrintStream(new FileOutputStream(tmpdir+"/myerr.txt"), false);
		ClassLoader classLoader = new URLClassLoader(new URL[] { new File(tmpdir).toURI().toURL() }, 
														ClassLoader.getSystemClassLoader());
		
		String input = null;
		while((input = scan.nextLine()).toLowerCase().compareTo("exit") != 0)
		{
			System.setErr(printErr);
			// first test; class member
			boolean isMethod = false;
			javaClassBuilder(currClassName, prevClassName, i, isMethod, input);
			boolean compileResult = compile(currClassName+".java");
			
			System.setErr(System.out);
			// second test; if first test fail
			if(!compileResult)
			{
				isMethod = true;
				javaClassBuilder(currClassName, prevClassName, i, isMethod, input);	
				compileResult = compile(currClassName+".java");
			}
			// if success load exec
			if(compileResult)
			{
				Class<?> aclass = classLoader.loadClass(currClassName);
				Method classMethod = aclass.getMethod("exec", (Class<?>[])null);
				classMethod.invoke(null, (Object[]) null);
				
				// Interp_number
				prevClassName = currClassName;
				currClassName = strName+(++i);
			}
			System.out.print("> ");
		}
		System.out.println();
		scan.close();
	}
	
	public static void javaClassBuilder(String currClassName, String prevClassName, int i, 
									boolean isMethod, String input) throws IOException
	{
		StringBuilder sb = new StringBuilder();			
		sb.append("import java.io.*;\r\n");
		sb.append("import java.util.*;\r\n");

		sb.append("public class ");
		sb.append(currClassName);
		if(i > 0)
		{
			sb.append(" extends ");
			sb.append(prevClassName);
		}
		sb.append("\r\n{\r\n");
		
		if(!isMethod)
		{
			sb.append("\tpublic static ");
			sb.append(input);
		}
		sb.append("\r\n\tpublic static void exec()");
		sb.append("\r\n\t{\r\n");
		sb.append("\t\t");
		if(isMethod)
		{
			sb.append(input);
		}
		sb.append("\r\n\t}\r\n");
		sb.append("}\r\n");
		
		String writeTo = tmpdir+"/"+currClassName+".java";
		BufferedWriter writer = new BufferedWriter(new FileWriter(writeTo));
		writer.write(sb.toString());
		writer.close();
		
		// print path & java code in current Interp_number
		if(detailprint)
		{
			System.out.println("write to : "+writeTo);
			System.out.println(sb.toString());
		}
	}
	
	public static final String CLASSPATH = System.getProperty("java.class.path");
	public static final String pathSep = System.getProperty("path.separator");
	public static String tmpdir = new File(System.getProperty("java.io.tmpdir"), "").getAbsolutePath();
	protected static boolean compile(String... fileNames) 
	{
		List<File> files = new ArrayList<File>();
		for (String fileName : fileNames) 
		{
			File f = new File(tmpdir, fileName);
			files.add(f);
		}
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);
		Iterable<String> compileOptions = Arrays.asList("-d", tmpdir, "-cp", tmpdir+pathSep+CLASSPATH);
		JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, compileOptions, null, compilationUnits);
		boolean ok = task.call();

		try 
		{
			fileManager.close();
		}
		catch (IOException ioe) 
		{
			ioe.printStackTrace(System.err);
		}

		return ok;
	}
}
