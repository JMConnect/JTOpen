import java.io.*;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;


public class Jdbc42Task extends MatchingTask
{
  static final String copyright = "Copyright (C) 2009-2019 International Business Machines Corporation and others.";

  private boolean verbose_;

  private File destDir_;  // destination directory -- contains files which will be reprocessed
                          // existing files will be overwritten by processed source files
  private File srcDir_;   // source directory where source files will come from

  private File sourcelist_; 
  
  private int numFilesProcessed_ = 0; // count of class files processed

  public void setSourcelist(File sourcelist)
  {
    sourcelist_ = sourcelist;
  }

  public void setDestdir(File destDir)
  {
    destDir_ = destDir;
  }
  
  public void setSrcdir (File srcDir) {
	  srcDir_ = srcDir; 
  }

  public void setVerbose(boolean verbose)
  {
    verbose_ = verbose;
  }


  // Executes the task.
  public void execute() throws BuildException
  {
	BuildException lastException = null; 
	ArrayList list = new ArrayList();
	try { 
		BufferedReader reader = new BufferedReader(new FileReader(sourcelist_)); 
		String line = reader.readLine(); 
		while (line != null) { 
			line = line.trim(); 
			if (line.length() > 0) {
				if (line.charAt(0)=='#') {
					// Skip 
				} else { 
					list.add(line);
				}
			}
			line = reader.readLine();
		}
	} catch (Exception e) { 
        if (verbose_) { 
       		System.out.println("Error ("+e+ ") processing "+sourcelist_);
       		e.printStackTrace(System.out); 
        }
		BuildException be = new BuildException("Error ("+e+ ") processing "+sourcelist_);
		be.initCause(e); 
		throw be; 
	}
	
    String[] destFileNames = new String[list.size()];
    for (int i = 0; i < destFileNames.length; i++) { 
    	destFileNames[i] = (String) list.get(i); 
    }

    for (int i=0; i<destFileNames.length; ++i)
    {
      String  filename = destFileNames[i]; 
      

      if (verbose_) System.out.println("Processing "+filename);

      try {
    	  if (filename.indexOf(".java") > 0) { 
    	    createJdbc42File(filename);
    	  }
      } catch (BuildException e) {
    	  System.out.println(e.toString());
    	  lastException = e; 
      }

      numFilesProcessed_++;
    }

    System.out.println("Number of files processed: " + numFilesProcessed_);
    if (lastException != null) throw lastException ;
  }
  
  final static int STATE_NONE=0; 
  final static int STATE_IFDEF40=1;
  final static int STATE_IFDEF42=2;
  final static int STATE_IFNDEF40=3; 
  final static int STATE_IFNDEF42=4; 
  
  final static int FOUND_NONE=0; 
  final static int FOUND_IFDEF42=1; 
  final static int FOUND_IFDEF40=2; 
  final static int FOUND_IFNDEF42=3;
  final static int FOUND_IFNDEF40=4;
  final static int FOUND_ENDIF=5; 
  final static int FOUND_COMMENT=6;        /* is any type of comment begin/end found */ 
  final static int FOUND_JDBC42DOC = 7; 
  final static int FOUND_JDBC40DOC = 8; 
  
  private int getLineType(String line, String filename, int lineNumber) { 
	  String originalLine = line; 
	  int typeCode = FOUND_NONE; 
	  line = line.trim(); 
	  if (line.indexOf("/*") == 0) {
		  line = line.substring(2).trim(); 
		  if (line.indexOf("ifdef")== 0) {
			  line = line.substring(5).trim(); 
			  if (line.indexOf("JDBC40") == 0) {
				  typeCode = FOUND_IFDEF40;
				  line = line.substring(6).trim();  
				  if (line.length() > 0) { 
					  System.out.println("Warning.  Incorrect ifdef line '"+originalLine+"' at "+filename+":"+lineNumber); 
				  }
			  } else  if (line.indexOf("JDBC42") == 0) {
				  typeCode = FOUND_IFDEF42;
				  line = line.substring(6).trim();  
				  if (line.length() > 0) { 
					  System.out.println("Warning.  Incorrect ifdef line '"+originalLine+"' at "+filename+":"+lineNumber); 
				  }
			  }
		  } else if (line.indexOf("ifndef")== 0) {
			  line = line.substring(6).trim(); 
			  if (line.indexOf("JDBC40") == 0) {
				  typeCode = FOUND_IFNDEF40;
				  line = line.substring(6).trim();  
				  if (line.indexOf("*/") != 0) {
					  System.out.println("Warning.  Incorrect ifndef line '"+originalLine+"' at "+filename+":"+lineNumber); 
				  }
			  }	else if (line.indexOf("JDBC42") == 0) {
				  typeCode = FOUND_IFNDEF42;
				  line = line.substring(6).trim();  
				  if (line.indexOf("*/") != 0) {
					  System.out.println("Warning.  Incorrect ifndef line '"+originalLine+"' at "+filename+":"+lineNumber); 
				  }	  
			  }
		  } else  if (line.indexOf("endif") == 0) {
			  line = line.substring(5).trim();
			  if (line.indexOf("*/") == 0) {
			     typeCode = FOUND_ENDIF; 
			  } else {
				  System.out.println("Warning.  Invalid /* endif */  structure: '"+line+"' at "+filename+":"+lineNumber); 
			  }
		  } else {
			  typeCode = FOUND_COMMENT; 
		  }
	  } else  if (line.indexOf("endif") == 0) {
		  line = line.substring(5).trim();
		  if (line.indexOf("*/") == 0) {
		     typeCode = FOUND_ENDIF; 
		  } else {
			  System.out.println("Warning.  Invalid endif */ structure: '"+line+"' at "+filename+":"+lineNumber); 
		  }
	  } else if (line.indexOf("endif") > 0) { 
		  System.out.println("Warning.  Invalid endif... structure: '"+line+"' at "+filename+":"+lineNumber); 
	  } else if (line.indexOf("//") == 0) {
		  line = line.substring(2).trim(); 
		  if (line.indexOf("JDBC40DOC") == 0) {
			  typeCode = FOUND_JDBC40DOC;  
		  } else if (line.indexOf("JDBC42DOC") == 0) {
				  typeCode = FOUND_JDBC42DOC;  
		  } else if (line.indexOf("JDBC40DOC") > 0) {
			  System.out.println("Warning.  Invalid JDBC40DOC after // line '"+line+"' at "+filename+":"+lineNumber); 
		  } else if (line.indexOf("JDBC42DOC") > 0) {
			  System.out.println("Warning.  Invalid JDBC42DOC after // line '"+line+"' at "+filename+":"+lineNumber); 
		  }
	  } else if (line.indexOf("JDBC40DOC") > 0) {
		  System.out.println("Warning.  Invalid JDBC40DOC line '"+line+"' at "+filename+":"+lineNumber); 
	  } else if (line.indexOf("JDBC42DOC") > 0) {
		  System.out.println("Warning.  Invalid JDBC42DOC line '"+line+"' at "+filename+":"+lineNumber); 
	  } else if (line.indexOf("/*") > 0) {
		  typeCode = FOUND_COMMENT; 
	  } else if (line.indexOf("*/") > 0) {
		  typeCode = FOUND_COMMENT; 
	  }
	  
	  return typeCode; 
  }
  
  private void createJdbc42File(String filename) throws BuildException {
   int lineNumber = 0;
   try
    {
      long start = System.currentTimeMillis();
      
      File inputFile  = new File(srcDir_+"/"+filename);
      File outputFile = new File(destDir_+"/"+filename); 
      File outputParent = outputFile.getParentFile(); 
      if (! outputParent.exists()) {
    	  if (verbose_) {
    		  System.out.println("Creating "+outputParent.getAbsolutePath()+" because it does not exist"); 
    	  }
    	  outputParent.mkdirs(); 
      }
      PrintWriter writer = new PrintWriter(new FileWriter(outputFile)); 
      BufferedReader reader = new BufferedReader(new FileReader(inputFile)); 
      
      int state = STATE_NONE; 
      int linetype = 0; 
      int stateChangeLineNumber = 0; 
      String line = reader.readLine(); 
      while (line != null) {
    	  lineNumber++; 
	      linetype = getLineType(line,filename,lineNumber); 
    	  switch(state) {
    	     case STATE_NONE:
    	    	 switch (linetype) {
    	    	    case FOUND_NONE:
    	    	    case FOUND_COMMENT: 
    	    		     break; 
    	    	    case FOUND_IFDEF40:
    	    	    	state = STATE_IFDEF40;
    	    	    	stateChangeLineNumber = lineNumber; 
    	    	    	line = "/* ifdef JDBC40 */";
    	    	    	break;
    	    	    case FOUND_IFDEF42:
    	    	    	state = STATE_IFDEF42;
    	    	    	stateChangeLineNumber = lineNumber; 
    	    	    	line = "/* ifdef JDBC42 */";
    	    	    	break;
    	    	    case FOUND_IFNDEF40:
    	    	    	state = STATE_IFNDEF40; 
    	    	    	stateChangeLineNumber = lineNumber; 
    	    	    	line = "/* ifndef JDBC40 ";
    	    	    	break;
    	    	    case FOUND_IFNDEF42:
    	    	    	state = STATE_IFNDEF42; 
    	    	    	stateChangeLineNumber = lineNumber; 
    	    	    	line = "/* ifndef JDBC42 ";
    	    	    	break;
    	    	    case FOUND_JDBC40DOC:
    	    	    	line = removeJdbc40Doc(line); 
    	    	    	break; 
    	    	    case FOUND_ENDIF:
 				        writer.close(); 
    	    	    	throw new Exception("FOUND INVALID ENDIF:"+stateChangeLineNumber+" '"+line+ "' AT "+filename+":" + lineNumber);
    	    	 }
    	    	 break;
    	     case STATE_IFDEF40:
    	     case STATE_IFDEF42:
					switch (linetype) {
					case FOUND_NONE:
						break;
					case FOUND_IFDEF40:
					case FOUND_IFNDEF40:
					case FOUND_IFDEF42:
					case FOUND_IFNDEF42:
 				        writer.close(); 
						throw new Exception("FOUND INVALID IFDEF (currently processing IFDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
					case FOUND_ENDIF:
						state = STATE_NONE;
    	    	    	stateChangeLineNumber = lineNumber; 
						line = "/* endif */ ";
						break;
					case FOUND_COMMENT:
						System.out.println("WARNING:  found comment in line '"+line+"' processing IFDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
						break; 
    	    	    case FOUND_JDBC40DOC:
    	    	    	line = removeJdbc40Doc(line); 
    	    	    	break; 
 					}
    	    	 break;
    	     case STATE_IFNDEF40:
					switch (linetype) {
					case FOUND_NONE:
						break;
					case FOUND_IFDEF40:
					case FOUND_IFNDEF40:
					case FOUND_IFDEF42:
					case FOUND_IFNDEF42:
 				        writer.close(); 
						throw new Exception("FOUND INVALID IFDEF (currently processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
					case FOUND_ENDIF:
						state = STATE_NONE;
    	    	    	stateChangeLineNumber = lineNumber; 
						line = " endif */ ";
						break;
					case FOUND_COMMENT:
						System.out.println("WARNING:  found comment in line '"+line+"' processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
						break; 
    	    	    case FOUND_JDBC40DOC:
						System.out.println("WARNING:  found JDBC40DOC in line '"+line+"' processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
    	    	    	
    	    	    	break; 
					}
    	    	 break;
    	     case STATE_IFNDEF42:
					switch (linetype) {
					case FOUND_NONE:
						break;
					case FOUND_IFDEF40:
					case FOUND_IFNDEF40:
					case FOUND_IFDEF42:
					case FOUND_IFNDEF42:
 				        writer.close(); 
						throw new Exception("FOUND INVALID IFDEF (currently processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
					case FOUND_ENDIF:
						state = STATE_NONE;
    	    	    	stateChangeLineNumber = lineNumber; 
						line = " endif */ ";
						break;
					case FOUND_COMMENT:
						System.out.println("WARNING:  found comment in line '"+line+"' processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
						break; 
    	    	    case FOUND_JDBC40DOC:
						System.out.println("WARNING:  found JDBC40DOC in line '"+line+"' processing IFNDEF:"+stateChangeLineNumber+") '" + line
								+ "' AT "+filename+":" + lineNumber);
    	    	    	
    	    	    	break; 
					}
    	    	 break;
    	  }
    	  writer.print(line+"\n"); 
    	  line = reader.readLine(); 
      }
      writer.close(); 
      
      long end = System.currentTimeMillis();

      if (verbose_) System.out.println("Processed. Time: "+(end-start)+" ms");
    }
    catch (java.io.CharConversionException e) {
    	System.out.println("CharConversionException processing "+filename+ " line : "+lineNumber);
    	e.printStackTrace(); 
    	Throwable cause = e.getCause(); 
    	while (cause != null ) { 
    		System.out.println("----------caused by --------------------");
    		cause.printStackTrace();
    		cause = cause.getCause(); 
    	}
    	throw new BuildException(e); 
    	
    }
    catch (Exception e) { 
    	System.out.println("Exception processing "+filename+ " line : "+lineNumber);
    	e.printStackTrace(); 
    	throw new BuildException(e); 
    }
  }

private String removeJdbc40Doc(String line) {
	int index = line.indexOf("JDBC40DOC"); 
	if (index > 0) {
		return line.substring(index+9); 
	}
	System.out.println("WARNING:  did not find JDBC40DOC when processing removeJdbc40Doc"); 
	return line; 
}


}
