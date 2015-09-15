package com.blackboard.SpringMVCB2;

import blackboard.admin.data.course.Enrollment;
import blackboard.admin.persist.course.EnrollmentLoader;
import blackboard.admin.persist.course.EnrollmentPersister;

import blackboard.data.course.Course;
import blackboard.data.ReceiptMessage;
import blackboard.data.ReceiptOptions;
import blackboard.data.user.User;

import blackboard.persist.Id;
import blackboard.persist.course.CourseDbLoader;
import blackboard.persist.course.CourseMembershipDbLoader;
import blackboard.persist.course.CourseMembershipDbPersister;

import blackboard.persist.user.UserDbLoader;


import blackboard.platform.gradebook2.*;

import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.servlet.InlineReceiptUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);
		
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		
		String formattedDate = dateFormat.format(date);
		
		model.addAttribute("serverTime", formattedDate );
		
		return "home";
	}

	@RequestMapping(value = "/learnhello", method = RequestMethod.GET)
	public String learnhello(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);
		
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		
		String formattedDate = dateFormat.format(date);
		
		model.addAttribute("serverTime", formattedDate );
		
		return "learnhello";
	}
	
        @RequestMapping(value = "/gst", method = RequestMethod.GET)
	public String gst(HttpServletRequest request, Locale locale, Model model) {
		logger.info("Grade Schema Test! The client locale is {}.", locale);
		CourseMembershipDbLoader memLoader;
		CourseDbLoader crsLoader;
                EnrollmentLoader enLoader;
                Id courseMemberId; // is this the useId or the membershipId?
                UserDbLoader usrLoader;
                
                ReceiptOptions rOpts = InlineReceiptUtil.getReceiptFromRequest(request);
                if(null==rOpts){
                    rOpts = new ReceiptOptions();
                }
                
                try {
                
                    GradebookManager gbMgr = GradebookManagerFactory.getInstanceWithoutSecurityCheck();
                    crsLoader = CourseDbLoader.Default.getInstance();
                    Course course = crsLoader.loadByCourseId("mbk-test-course");
                    
                    usrLoader = UserDbLoader.Default.getInstance();
                    User user = usrLoader.loadByUserName("mbk");
                    
                    GradeWithAttemptScore courseGrade = gbMgr.getCourseGrade( course.getId(), user.getId() );

                    Date date = new Date();
                    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
                    String formattedDate = dateFormat.format(date);
                    model.addAttribute("serverTime", formattedDate );
                    rOpts.addSuccessMessage("All is well! CourseId:" + course.getCourseId() + " courseGrade:" + courseGrade.toString());
                } catch (Exception e) {
                    rOpts.addErrorMessage("Had an exception!", e);
                    e.printStackTrace();
                }
                InlineReceiptUtil.addReceiptToRequest(request, rOpts);
		return "gst"; // grade schema test
	} // end public String gst(
        
}// end public class HomeController
