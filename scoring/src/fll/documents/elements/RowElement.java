package fll.documents.elements;

import java.util.Iterator;
import java.util.List;

import fll.documents.writers.SubjectiveConstants;
import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.RubricRange;


/**
 * This represents a row pair in the pdf rubric table.  The top row is colored for the subjective category and is 
 * labeled with the rows topic on the left side.  On the right side of the same colored row is the description
 * of the rows topic.  The second row is an ND and then 4 descriptions describing the depth of the teams ability 
 * for the rows topic.
 * 
 * For example
 * <p>
 * Row 1 &lt;blue row color&gt;Discovery | Balanced emphasis on all three aspects of FLL &lt;end blue row color&gt;<br>
 * Row 2 ND | emphasis on only one aspect | emphasis on two aspects | emphasis o all three | balanced emphasis on all three
 * </p>
 *
 */
public class RowElement {
	//Inspiration for example (90 degrees on the left side of the table)
	String catagory = null;
	//Discovery for example (left side of the colored row)
	String rowTitle = null;
	//Balanced emphasis on all three aspects (Robot, Project, Core Values) of FLL; its not just about winning awards (right side of the colored row)
	String rowDescription = null;
	//Beginning description, 2nd row of row element
	String beginningDescription = null;
	//Developing description, 2nd row of row element
	String developingDescription = null;
	//Accomplished description, 2nd row of row element
	String accomplishedDescription = null;
	//Exemplary description, 2nd row of row element
	String exemplaryDescription = null;
	
	public RowElement (AbstractGoal abstractGoal) {
		this.catagory = ((Goal)abstractGoal).getCategory();
		this.rowTitle = abstractGoal.getTitle();
		this.rowDescription = abstractGoal.getDescription().replaceAll("\\s+", " ").replaceAll("\n", "");
		
		List<RubricRange> rubricRangeElements = ((Goal)abstractGoal).getRubric();
		RubricRange rubric = null;
		for (Iterator<RubricRange> iterator = rubricRangeElements.iterator(); iterator.hasNext();) {
			rubric = iterator.next();
			
			switch (rubric.getTitle()) {
				case SubjectiveConstants.BEGINNING:
					this.beginningDescription = rubric.getShortDescription();;
					break;
				case SubjectiveConstants.DEVELOPING:
					this.developingDescription = rubric.getShortDescription();;
					break;
				case SubjectiveConstants.ACCOMPLISHED:
					this.accomplishedDescription = rubric.getShortDescription();;
					break;
				case SubjectiveConstants.EXEMPLARY:
					this.exemplaryDescription = rubric.getShortDescription();;
					break;
				default:
					break;
			}
		}
	}
	
	public int secondIndexOfCL(String str) {        
	    for(int i=1; i<str.length(); i++) {
	    	System.out.print(str.charAt(i));
	    	if (Character.isUpperCase(str.charAt(i)) && Character.isUpperCase(str.charAt(i+1))) {
	    		System.out.println("substring 2: " + str.substring(i, i+2));
	    		System.out.println("substring 3: " + str.substring(i, i+3));
	    		if (str.substring(i, i+2).equals("OR")) {
	    			i = i + 2;
	    		} else if (str.substring(i, i+3).equals("AND")) {
	    			i = i + 3;
	    		} else if (str.substring(i, i+3).equals("FLL")) {
	    			i = i + 3;
	    		} else if (str.substring(i, i+3).equals("1//2")) {
	    			i = i + 3;
	    		}
	    		continue;
	    	}
	        if(Character.isUpperCase(str.charAt(i)) && !Character.isUpperCase(str.charAt(i+1))) {
	        	System.out.println("i: " + str.charAt(i));
	            return i;
	        }
	    }
	    return str.length();
	}

	public String toString() {
		String NL = "\n";
		StringBuffer output = new StringBuffer();
		output.append("-RowElementStart----------------------------------");
		output.append(NL);
		output.append("Catagory: ");
		output.append(catagory);
		output.append(NL);
		output.append("Row Title: ");
		output.append(rowTitle);
		output.append(NL);
		output.append("Row Description: ");
		output.append(rowDescription);
		output.append(NL);
		output.append("----------------------------------" + NL);
		output.append(SubjectiveConstants.BEGINNING + ":");
		output.append(beginningDescription + NL);
		output.append(SubjectiveConstants.DEVELOPING + ":");
		output.append(developingDescription + NL);
		output.append(SubjectiveConstants.ACCOMPLISHED + ":");
		output.append(accomplishedDescription + NL);
		output.append(SubjectiveConstants.EXEMPLARY + ":");
		output.append(exemplaryDescription + NL);
		output.append("-RowElementEnd----------------------------------" + NL);
		return output.toString();
	}
		
	public String getCatagory() {
		return catagory;
	}

	public String getRowTitle() {
		return rowTitle;
	}

	public String getDescription() {
		return rowDescription;
	}

	public String getBeginningDescription() {
		return beginningDescription;
	}

	public String getDevelopingDescription() {
		return developingDescription;
	}

	public String getAccomplishedDescription() {
		return accomplishedDescription;
	}

	public String getExemplaryDescription() {
		return exemplaryDescription;
	}
}
