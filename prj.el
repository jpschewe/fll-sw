;;This file assumes that the following variables and methods exist:
(jde-set-project-name "fll-2002")
(let ((project-root (file-name-directory load-file-name))
      (catalina-home "/opt/jakarta"))
  (jde-set-variables
   '(jde-run-working-directory (expand-file-name "build/" project-root))
   '(jde-compile-option-directory (expand-file-name "build/" project-root))
   '(jde-run-read-app-args t)
   '(jde-global-classpath (list
			   (expand-file-name "build/WEB-INF/classes" project-root)
			   (expand-file-name "build/library/classes" project-root)
			   
			   (expand-file-name "web/WEB-INF/lib/mm.mysql-2.0.14-bin.jar" project-root)
			   (expand-file-name "web/WEB-INF/lib/JonsInfra-0.2.jar" project-root)

			   (expand-file-name "web/common-lib/xercesImpl.jar" project-root)
			   (expand-file-name "web/common-lib/xmlParserAPIs.jar" project-root)
			   
			   (expand-file-name "common/lib/servlet.jar" catalina-home)
			   (expand-file-name "common/lib/xercesImpl.jar" catalina-home)
			   (expand-file-name "common/lib/xmlParserAPIs.jar" catalina-home)
			   ))
   
   '(jde-compile-option-deprecation t)
   '(jde-run-option-vm-args '("-DASSERT_BEHAVIOR=CONTINUE "))
 '(jde-gen-buffer-boilerplate (quote (
				      "/*"
				      " * Copyright (c) 2000-2002 INSciTE.  All rights reserved"
				      " * INSciTE is on the web at: http://www.hightechkids.org"
				      " * This code is released under GPL; see LICENSE.txt for details."
				      " */"
                                      )))
 '(jde-gen-class-buffer-template 
   (list 
    "(funcall jde-gen-boilerplate-function)"
    "(jde-gen-get-package-statement)"
    "\"/**\" '>'n"
    "\" * Add class comment here!\" '>'n"
    "\" *\" '>'n"
    "\" * @version $Revision$\" '>'n"
    "\" */\" '>'n'"
    "\"public class \"" 
    "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" 
    "\" \" (jde-gen-get-extend-class)" 
    "\"{\" '>'n"
    "'>'n"
    "\"public \"" 
    "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" 
    "\"()\"" 
    "\" {\" '>'n" 
    "'>'n" 
    "\"}\" '>'n" 
    "\"}\" '>'n")) 
   ))
