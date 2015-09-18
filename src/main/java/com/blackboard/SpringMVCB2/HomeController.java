package com.blackboard.SpringMVCB2;

import blackboard.admin.data.course.Enrollment;
import blackboard.admin.persist.course.EnrollmentLoader;
import blackboard.admin.persist.course.EnrollmentPersister;

import blackboard.data.course.Course;
import blackboard.data.course.CourseMembership;
import blackboard.data.ReceiptMessage;
import blackboard.data.ReceiptOptions;
import blackboard.data.user.User;

import blackboard.persist.Id;
import blackboard.persist.course.CourseDbLoader;
import blackboard.persist.course.CourseMembershipDbLoader;
import blackboard.persist.course.CourseMembershipDbPersister;

import blackboard.persist.user.UserDbLoader;


import blackboard.platform.gradebook2.*;
import blackboard.platform.gradebook2.impl.GradingSchemaDAO;
import blackboard.platform.gradebook2.impl.GradingSchemeSymbolDAO;

import blackboard.platform.log.LogServiceFactory;

import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.servlet.InlineReceiptUtil;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import java.util.*;

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
                BookData bookData;
		CourseMembershipDbLoader crsMemLoader;
		CourseDbLoader crsLoader;
                EnrollmentLoader enLoader;
                Id courseMemberId; // is this the useId or the membershipId?
                UserDbLoader usrLoader;
                
                ReceiptOptions rOpts = InlineReceiptUtil.getReceiptFromRequest(request);
                if(null==rOpts){
                    rOpts = new ReceiptOptions();
                }
                
                try {
                     // Version 1.0.1 modified to also show GradingSchema.getSChemaValue to display the value getSchemaValue of the default Schema named 'Letter'.
                     // Version 1.0.2 modified to show crsmain PK1.
                    GradebookManager gradebookManager = GradebookManagerFactory.getInstanceWithoutSecurityCheck();
                    crsLoader = CourseDbLoader.Default.getInstance();
                    Course course = crsLoader.loadByCourseId("mbk-test-course");
                    Id courseId = course.getId(); 
                    String courseIdString = course.getCourseId();
                    

                    usrLoader = UserDbLoader.Default.getInstance();
                    User user = usrLoader.loadByUserName("mbk");
                    
                    crsMemLoader = CourseMembershipDbLoader.Default.getInstance();
                    CourseMembership cm = crsMemLoader.loadByCourseAndUserId(course.getId(), user.getId());
                    
                    // Gradebook code here.. leveraged from Lance Neumann's email AmarilloTest.java Feb 11, 2015
                    bookData = gradebookManager.getBookData(new BookDataRequest(courseId));
                    //it is necessary to execute these two methods to obtain calculated scores and extended grade data
                    bookData.addParentReferences();
                    bookData.runCumulativeGrading();
                    GradebookSettings gbs = gradebookManager.getGradebookSettings(courseId);
                    // get column that is marked external
                    GradableItem gi = gradebookManager.getGradebookItem(gbs.getPublicItemId());
                    Id secondaryId = gi.getSecondaryGradingSchemaId();
                    GradingSchema realSchema = GradingSchemaDAO.get().loadById(secondaryId);
                    realSchema.setSymbols( GradingSchemeSymbolDAO.get().getSymbolsForSchema( secondaryId ) );
                    // get type of grade to format for display
                    String gradeType = GradingSchemaDAO.get().loadById(gi.getGradingSchemaId()).getScaleType().name();
                    // get the gradingSchemas for the course
                    List<GradingSchema> gradingSchemas = GradingSchemaDAO.get().getGradingSchemaByCourse(courseId);
                    // Look for the grading schema this test was built to work with mbk-schema
                    GradingSchema selectedGradingSchema = null;
                    if(gradingSchemas!=null){
                        if(gradingSchemas.size()==1){
                            selectedGradingSchema = gradingSchemas.get(0);
                        } else {
                            for (GradingSchema gradingSchema : gradingSchemas) {
                                if("mbk-schema".equalsIgnoreCase(gradingSchema.getLocalizedTitle())){
                                    selectedGradingSchema = gradingSchema;
                                    break;
                                }
                            }  // end for (GradingSchema...                 
                        } // end else
                    } // end if(gradingSchemas!=null)
                    
                    // for grade formatting
                    DecimalFormat twoPlacesFormat = new DecimalFormat("#0.00");
                    
                    GradeWithAttemptScore gwas2 = bookData.get(cm.getId(), gi.getId());
                    String studentGrade = "";
                    String studentId = cm.getId().toExternalString();
                    

                    if(gwas2 == null || gwas2.isNullGrade()) {
                        studentGrade = "-";
                    } else {
                        String numGrade = null;

                        if(gradeType.equalsIgnoreCase("COMPLETE")) {
                            // don't need to retrieve score
                        } else if(gradeType.equalsIgnoreCase("PERCENT")) {
                            //                      numGrade = twoPlacesFormat.format(gwas2.getScoreValue());
                            numGrade = twoPlacesFormat.format((gwas2.getScoreValue()/gwas2.getPointsPossible())*100)+"%";
                        } else if(gradeType.equalsIgnoreCase("SCORE")) {
                            numGrade = twoPlacesFormat.format(gwas2.getScoreValue());
                        } else if(gradeType.equalsIgnoreCase("TABULAR")) {
                            numGrade = gwas2.getSchemaValue();
                        } else if(gradeType.equalsIgnoreCase("TEXT")) {
                            numGrade = gwas2.getTextValue();
                        }

                        // get mbk-schema grades for the scores
                        if(numGrade!=null && selectedGradingSchema!=null){
                            Double scoreValue = gwas2.getScoreValue();

                            Double pp = gwas2.getPointsPossible();
                            
                            // The following is the selected grading schema's (mbk-schema) letter grade.
                            String letterGrade = selectedGradingSchema.getSchemaValue(scoreValue, pp);
                            // The following is Learns built in Letter schema. Not used for this demo.
                            String schemaValue = realSchema.getSchemaValue(scoreValue, pp); 
                            if(letterGrade!=null && !letterGrade.trim().equals("")){
                                studentGrade = "GradingSchema.getSchemaValue:" + schemaValue + ", numGrade:" + numGrade + ", selectedGradingSchema.getSchemaValue:" + letterGrade;
                            }// end if (letterGrade!=null...
                        }// end if(numGrade!=null...
                    }// end if(gwas2 == null || gwas2.isNullGrade()) { ...else {
                  
                    
                    // The following is successful, but not what we need to demonstrate the issue.
                    GradeWithAttemptScore courseGrade = gradebookManager.getCourseGrade( course.getId(), cm.getId() );
                    
                    String schemaValue = courseGrade.getSchemaValue();
                    double scoreValue = courseGrade.getScoreValue();
                    // End Gradebook code...
                    
                    Date date = new Date();
                    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
                    String formattedDate = dateFormat.format(date);
                    model.addAttribute("serverTime", formattedDate );
                    // rOpts.addSuccessMessage("All is well! scoreValue:" + Double.toString(scoreValue) + " schemaValue:" + schemaValue);
                    rOpts.addSuccessMessage("All is well! courseIdString:" + courseIdString + " StudentGrade: " + studentGrade);
                } catch (Exception e) {
                    rOpts.addErrorMessage("Had an exception!", e);
                    e.printStackTrace();
                }
                InlineReceiptUtil.addReceiptToRequest(request, rOpts);
		return "gst"; // grade schema test
	} // end public String gst(
        
        
        
}// end public class HomeController
