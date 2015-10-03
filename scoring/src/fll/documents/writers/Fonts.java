package fll.documents.writers;

import com.itextpdf.text.Font;

public class Fonts {
  private static Font f7 = new Font(Font.FontFamily.HELVETICA, 7);
	private static Font f8 = new Font(Font.FontFamily.HELVETICA, 8);
	private static Font f9 = new Font(Font.FontFamily.HELVETICA, 9);
	
	public static Font getFont(FontSizes fs) {
		switch (fs) {
			case CORE_VALUE:
				return f8;
			case PROGRAMMING:
				return f8;
			case PROJECT:
				return f7;
			case DESIGN:
				return f9;
			default:
				return f8;
			}
	}
	
	public static int getCommentSize(FontSizes fs) {
		switch (fs) {
			case CORE_VALUE:
				return 1;
			case PROGRAMMING:
				return 2;
			case PROJECT:
				return 2;
			case DESIGN:
				return 2;
			default:
				return 1;
			}
	}
}


