// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/Attic/MGRSPoint.java,v $
// $RCSfile: MGRSPoint.java,v $
// $Revision: 1.2 $
// $Date: 2003/02/26 00:29:26 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap;

import com.bbn.openmap.proj.Ellipsoid;
import com.bbn.openmap.proj.ProjMath;
import com.bbn.openmap.util.ArgParser;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.omGraphics.*;

/**
 * A class representing a MGRS coordinate that has the ability to
 * provide the decimal degree lat/lon equivalent, as well as the UTM
 * equivalent.  This class does not do checks to see if the MGRS
 * coordiantes provided actually make sense.  It assumes that the
 * values are valid.
 */
public class MGRSPoint extends UTMPoint {

    /** UTM zones are grouped, and assigned to one of a group of 6 sets. */
    protected final static int NUM_100K_SETS = 6;
    /** The column letters (for easting) of the lower left value, per set. */
    public final static int[] SET_ORIGIN_COLUMN_LETTERS = {'A', 'J', 'S', 'A', 'J', 'S'};
    /** The row letters (for northing) of the lower left value, per set. */
    public final static int[] SET_ORIGIN_ROW_LETTERS = {'A', 'F', 'A', 'F', 'A', 'F'};
    /**
     * The column letters (for easting) of the lower left value, per
     * set,, for Bessel Ellipsoid. 
     */
    public final static int[] BESSEL_SET_ORIGIN_COLUMN_LETTERS = {'A', 'J', 'S', 'A', 'J', 'S'};
    /**
     * The row letters (for northing) of the lower left value, per
     * set, for Bessel Ellipsoid. 
     */
    public final static int[] BESSEL_SET_ORIGIN_ROW_LETTERS = {'L', 'R', 'L', 'R', 'L', 'R'};

    public final static int SET_NORTHING_ROLLOVER = 20000000;
    /** Use 5 digits for northing and easting values, for 1 meter accuracy of coordinate. */
    public final static int ACCURACY_1_METER = 5;
    /** Use 4 digits for northing and easting values, for 10 meter accuracy of coordinate. */
    public final static int ACCURACY_10_METER = 4;
    /** Use 3 digits for northing and easting values, for 100 meter accuracy of coordinate. */
    public final static int ACCURACY_100_METER = 3;
    /** Use 2 digits for northing and easting values, for 1000 meter accuracy of coordinate. */
    public final static int ACCURACY_1000_METER = 2;
    /** Use 1 digits for northing and easting values, for 10000 meter accuracy of coordinate. */
    public final static int ACCURACY_10000_METER = 1;

    /** The set origin column letters to use. */
    protected int[] originColumnLetters = SET_ORIGIN_COLUMN_LETTERS;
    /** The set origin row letters to use. */
    protected int[] originRowLetters = SET_ORIGIN_ROW_LETTERS;

    public final static int A = 'A';
    public final static int I = 'I';
    public final static int O = 'O';
    public final static int V = 'V';
    public final static int Z = 'Z';

    protected boolean DEBUG = false;

    /** The String holding the MGRS coordinate value. */
    protected String mgrs;

    /**
     * Controls the number of digits that the MGRS coordinate will
     * have, which directly affects the accuracy of the coordinate.
     * Default is ACCURACY_1_METER, which indicates that MGRS
     * coordinates will have 10 digits (5 easting, 5 northing) after
     * the 100k two letter code, indicating 1 meter resolution.
     */
    protected int accuracy = ACCURACY_1_METER;

    /**
     * Point to create if you are going to use the static methods to
     * fill the values in.  
     */
    public MGRSPoint() {
	DEBUG = Debug.debugging("mgrs");
    }

    /**
     * Constructs a new MGRS instance from a MGRS String.
     */
    public MGRSPoint(String mgrsString) {
	this();
	setMGRS(mgrsString);
    }

    /**
     * Contructs a new MGRSPoint instance from values in another MGRSPoint.
     */
    public MGRSPoint(MGRSPoint point) {
	this();
	mgrs = point.mgrs;
	northing = point.northing;
	easting = point.easting;
	zone_number = point.zone_number;
	zone_letter = point.zone_letter;
    }

    public MGRSPoint(float northing, float easting, int zoneNumber, char zoneLetter) {
	super(northing, easting, zoneNumber, zoneLetter);
	
    }

    /**
     * Contruct a MGRSPoint from a LatLonPoint, assuming a WGS_84 ellipsoid.
     */
    public MGRSPoint(LatLonPoint llpoint) {
	this(llpoint, Ellipsoid.WGS_84);
    }

    /**
     * Construct a MGRSPoint from a LatLonPoint and a particular
     * ellipsoid.  
     */
    public MGRSPoint(LatLonPoint llpoint, Ellipsoid ellip) {
	this();
	LLtoMGRS(llpoint, ellip, this);
    }

    /**
     * Set the MGRS value for this Point.  Will be decoded, and the
     * UTM values figured out.  You can call toLatLonPoint() to
     * translate it to lat/lon decimal degrees.
     */
    public void setMGRS(String mgrsString) {
	mgrs = mgrsString.toUpperCase(); // Just to make sure.
	try {
	    decode(mgrs);
	} catch (StringIndexOutOfBoundsException sioobe) {
	    throw new NumberFormatException("MGRSPoint has bad string: " + mgrsString);
	}
    }

    /**
     * Get the MGRS string value - the honkin' coordinate value.
     */
    public String getMGRS() {
	return mgrs;
    }

    /**
     * Convert this MGRSPoint to a LatLonPoint, and assume a WGS_84
     * ellisoid.
     */
    public LatLonPoint toLatLonPoint() {
	return toLatLonPoint(Ellipsoid.WGS_84, new LatLonPoint());
    }

    /**
     * Convert this MGRSPoint to a LatLonPoint, and use the given
     * ellipsoid.
     */
    public LatLonPoint toLatLonPoint(Ellipsoid ellip) {
	return toLatLonPoint(ellip, new LatLonPoint());
    }

    /**
     * Fill in the given LatLonPoint with the converted values of this
     * MGRSPoint, and use the given ellipsoid.  
     */
    public LatLonPoint toLatLonPoint(Ellipsoid ellip, LatLonPoint llpoint) {
	return MGRStoLL(this, ellip, llpoint);
    }

    /**
     * Returns a string representation of the object.
     * @return String representation
     */
    public String toString() {
	return "MGRSPoint[" + mgrs +"]";
    }

    /**
     * Create a LatLonPoint from a MGRSPoint.
     * @param mrgsp to convert.
     * @param ellip Ellipsoid for earth model.
     * @param llp a LatLonPoint to fill in values for.  If null, a new
     * LatLonPoint will be returned.  If not null, the new values will
     * be set in this object, and it will be returned.
     * @return LatLonPoint with values converted from MGRS coordinate.
     */
    public static LatLonPoint MGRStoLL(MGRSPoint mgrsp, Ellipsoid ellip, LatLonPoint llp) {
	return UTMtoLL(mgrsp, ellip, llp);
    }

    /**
     * Converts a LatLonPoint to a MGRS Point, assuming the WGS_84 ellipsoid.
     * @return MGRSPoint, or null if something bad happened.
     */
    public static MGRSPoint LLtoMGRS(LatLonPoint llpoint) {
	return LLtoMGRS(llpoint, Ellipsoid.WGS_84, new MGRSPoint());
    }

    /**
     * Converts a LatLonPoint to a MGRS Point.
     * @param llpoint the LatLonPoint to convert.
     * @param mgrsp a MGRSPoint to put the results in.  If it's
     * null, a MGRSPoint will be allocated.
     * @return MGRSPoint, or null if something bad happened.  If a
     * MGRSPoint was passed in, it will also be returned on a
     * successful conversion.
     */
    public static MGRSPoint LLtoMGRS(LatLonPoint llpoint, MGRSPoint mgrsp) {
	return LLtoMGRS(llpoint, Ellipsoid.WGS_84, mgrsp);
    }

    /**
     * Create a MGRSPoint from a LatLonPoint.
     * @param llp LatLonPoint to convert.
     * @param ellip Ellipsoid for earth model.
     * @param mgrsp a MGRSPoint to fill in values for.  If null, a new
     * MGRSPoint will be returned.  If not null, the new values will
     * be set in this object, and it will be returned.
     * @return MGRSPoint with values converted from lat/lon.
     */
    public static MGRSPoint LLtoMGRS(LatLonPoint llp, Ellipsoid ellip, MGRSPoint mgrsp) {
	mgrsp = (MGRSPoint)LLtoUTM(llp, ellip, mgrsp);
	mgrsp.resolve();
	return mgrsp;
    }

    /**
     * Set the number of digits to use for easting and northing
     * numbers in the mgrs string, which reflects the accuracy of the
     * corrdinate.  From 5 (1 meter) to 1 (10,000 meter).
     */
    public void setAccuracy(int value) {
	accuracy = value;
    }

    public int getAccuracy() {
	return accuracy;
    }

    /**
     * Set the UTM parameters from a MGRS string.
     */
    protected void decode(String mgrsString) 
	throws NumberFormatException {

	if (mgrsString == null || mgrsString.length() == 0) {
	    throw new NumberFormatException("MGRSPoint coverting from nothing");
	}

	int length = mgrsString.length();

	String hunK = null;
	String seasting = null;
	String snorthing = null;

	StringBuffer sb = new StringBuffer();
	char testChar;
	int i = 0;

	// get Zone number
	while (!Character.isLetter(testChar = mgrsString.charAt(i))) {
	    if (i > 2) {
		throw new NumberFormatException("MGRSPoint bad conversion from: " + mgrsString);
	    }
	    sb.append(testChar);
	    i++;
	}

	zone_number = Integer.parseInt(sb.toString());

	if (i == 0 || i+3 > length) {
	    // A good MGRS string has to be 4-5 digits long, ##AAA/#AAA at least.
	    throw new NumberFormatException("MGRSPoint bad conversion from: " + mgrsString);
	}

	zone_letter = mgrsString.charAt(i++);

	// Should we check the zone letter here?  Why not.
	if (zone_letter <= 'A' ||
	    zone_letter == 'B' ||
	    zone_letter == 'Y' ||
	    zone_letter >= 'Z' ||
	    zone_letter == 'I' ||
	    zone_letter == 'O') {
	    throw new NumberFormatException("MGRSPoint zone letter " + 
					    (char)zone_letter + " not handled: " + 
					    mgrsString);
	}

	hunK = mgrsString.substring(i, i+=2);

	int set =  get100kSetForZone(zone_number);

	float east100k = getEastingFromChar(hunK.charAt(0), set);
	float north100k = getNorthingFromChar(hunK.charAt(1), set);

	// We have a bug where the northing may be 2000000 too low.  How 
	// do we know when to roll over?  Every 18 degrees. Q - 3, S -
	// 5, U - 5, W - 8

	if ((zone_letter == 'Q' && north100k < 1700000) ||
	    (zone_letter >= 'R')) north100k += 2000000;

	if ((zone_letter == 'S' && north100k < 3000000) ||
	    (zone_letter >= 'T')) north100k += 2000000;

	if ((zone_letter == 'U' && north100k < 5330000) ||
	    (zone_letter >= 'V')) north100k += 2000000;

	if (zone_letter >= 'X') {
	    north100k += 2000000;
	    if (north100k > 9500000) {
		// There's a tiny sliver of space that does this...
		north100k -= 2000000;
	    }
	}


	// This (^^) may not be enough, we have to find out
	// when we cross over a 18 degree line.

	// calculate the char index for easting/northing separator
	int remainder = length - i;

	if (remainder%2 != 0) {
	    throw new NumberFormatException("MGRSPoint has to have an even number \nof digits after the zone letter and two 100km letters - front \nhalf for easting meters, second half for \nnorthing meters" + mgrsString);
	}
	    
	int sep = remainder / 2;

	float sepEasting = 0f;
	float sepNorthing = 0f;

	if (sep > 0) {
	    float accuracyBonus = 100000f/(float)Math.pow(10, sep);

	    String sepEastingString = mgrsString.substring(i, i + sep);
	    sepEasting = Float.parseFloat(sepEastingString) * accuracyBonus;
	    String sepNorthingString = mgrsString.substring(i + sep);
	    sepNorthing = Float.parseFloat(sepNorthingString) * accuracyBonus;
	}
	
	easting = sepEasting + east100k;
	northing = sepNorthing + north100k;

	if (DEBUG) {
	    Debug.output("Decoded " + mgrsString + 
			 " as zone number: " + zone_number + 
			 ", zone letter: " + zone_letter + 
			 ", easting: " + easting +
			 ", northing: " + northing	+ 
			 ", 100k: " + hunK);
	}
    }

    /**
     * Create the mgrs string based on the internal UTM settings,
     * using the accuracy set in the MGRSPoint.
     */
    protected void resolve() {
	resolve(accuracy);
    }

    /**
     * Create the mgrs string based on the internal UTM settings.
     * @param accuracy The number of digits to use for the northing
     * and easting numbers.  5 digits reflect a 1 meter accuracy, 4 -
     * 10 meter, 3 - 100 meter, 2 - 1000 meter, 1 - 10,000 meter.
     */
    protected void resolve(int digitAccuracy) {
	StringBuffer sb = 
	    new StringBuffer(zone_number + "" + (char)zone_letter + 
			     get100kID(easting, northing, zone_number));
	StringBuffer seasting = new StringBuffer(Integer.toString((int)easting));
	StringBuffer snorthing = new StringBuffer(Integer.toString((int)northing));
	
	while (digitAccuracy > seasting.length()) {
	    seasting.insert(0, '0');
	}

	while (digitAccuracy > snorthing.length()) {
	    snorthing.insert(0, '0');
	}

	sb.append(seasting.substring(1, digitAccuracy + 1) + 
		  snorthing.substring(1, digitAccuracy + 1));
	
	mgrs = sb.toString();
    }

    /**
     * Given a UTM zone number, figure out the MGRS 100K set it is in.
     */
    protected int get100kSetForZone(int i) {
	int set = i%NUM_100K_SETS;
	if (set == 0) set = NUM_100K_SETS;
	return set;
    }

    /**
     * Provided so that extensions to this class can provide different
     * origin letters, in case of different ellipsoids. The int[]
     * represents all of the first letters in the bottom left corner
     * of each set box, as shown in an MGRS 100K box layout.
     */
    protected int[] getOriginColumnLetters() {
	return originColumnLetters;
    }

    /**
     * Provided so that extensions to this class can provide different
     * origin letters, in case of different ellipsoids. The int[]
     * represents all of the first letters in the bottom left corner
     * of each set box, as shown in an MGRS 100K box layout.
     */
    protected void setOriginColumnLetters(int[] letters) {
	originColumnLetters = letters;
    }

    /**
     * Provided so that extensions to this class can provide different
     * origin letters, in case of different ellipsoids.  The int[]
     * represents all of the second letters in the bottom left corner
     * of each set box, as shown in an MGRS 100K box layout.
     */
    protected int[] getOriginRowLetters() {
	return originRowLetters;
    }

    /**
     * Provided so that extensions to this class can provide different
     * origin letters, in case of different ellipsoids.  The int[]
     * represents all of the second letters in the bottom left corner
     * of each set box, as shown in an MGRS 100K box layout.
     */
    protected void setOriginRowLetters(int[] letters) {
	originRowLetters = letters;
    }

    /**
     * Get the two letter 100k designator for a given UTM easting,
     * northing and zone number value.
     */
    protected String get100kID(float easting, float northing, int zone_number) {
	int set = get100kSetForZone(zone_number);
	int setColumn = ((int)easting/100000);
	int setRow = ((int)northing/100000)%20;
	return get100kID(setColumn, setRow, set);
    }

    /**
     * Given the first letter from a two-letter MGRS 100k zone, and
     * given the MGRS table set for the zone number, figure out the
     * easting value that should be added to the other, secondary
     * easting value.
     */
    protected float getEastingFromChar(char e, int set) {
	int baseCol[] = getOriginColumnLetters();
	// colOrigin is the letter at the origin of the set for the column
	int curCol = baseCol[set-1];
	float eastingValue = 100000f;

	while (curCol != e) {
	    curCol++;
	    if (curCol == I) curCol++;
	    if (curCol == O) curCol++;
	    if (curCol > Z) curCol = A;
	    eastingValue += 100000f;
	}

	if (DEBUG) {
	    Debug.output("Easting value for " + (char)e + 
			 " from set: " + set + ", col: " + curCol + 
			 " is " + eastingValue);
	}
	return eastingValue;
    }
    
    /**
     * Given the second letter from a two-letter MGRS 100k zone, and
     * given the MGRS table set for the zone number, figure out the
     * northing value that should be added to the other, secondary
     * northing value.  You have to remember that Northings are
     * determined from the equator, and the vertical cycle of letters
     * mean a 2000000 additional northing meters.  This happens
     * approx. every 18 degrees of latitude.  This method does *NOT*
     * count any additional northings.  You have to figure out how
     * many 2000000 meters need to be added for the zone letter of the
     * MGRS coordinate.
     *
     * @param n second letter of the MGRS 100k zone
     * @param set the MGRS table set number, which is dependent on the
     * UTM zone number.
     */
    protected float getNorthingFromChar(char n, int set) {
	int baseRow[] = getOriginRowLetters();
	// rowOrigin is the letter at the origin of the set for the column
	int curRow = baseRow[set-1];
	float northingValue = 0f;

	while (curRow != n) {
	    curRow++;
	    if (curRow == I) curRow++;
	    if (curRow == O) curRow++;
	    if (curRow > V) curRow = A;
	    northingValue += 100000f;
	}

	if (DEBUG) {
	    Debug.output("Northing value for " + (char)n + 
			 " from set: " + set + ", row: " + curRow + 
			 " is " + northingValue);
	}

	return northingValue;
    }

    /**
     * Get the two-letter MGRS 100k designator given information
     * translated from the UTM northing, easting and zone number.
     * @param setColumn the column index as it relates to the MGRS
     * 100k set spreadsheet, created from the UTM easting.  Values are 1-8.
     * @param setRow the row index as it relates to the MGRS 100k set
     * spreadsheet, created from the UTM northing value.  Values are from 0-19.
     * @param set the set block, as it relates to the MGRS 100k set
     * spreadsheet, created from the UTM zone.  Values are from 1-60.
     * @return two letter MGRS 100k code.
     */
    protected String get100kID(int setColumn, int setRow, int set) {

	if (DEBUG) {
	    System.out.println("set (" + set + ") column = " + setColumn + ", row = " + setRow);
	}

	int baseCol[] = getOriginColumnLetters();
	int baseRow[] = getOriginRowLetters();

	// colOrigin and rowOrigin are the letters at the origin of the set
	int colOrigin = baseCol[set-1];
	int rowOrigin = baseRow[set-1];

	if (DEBUG) {
	    System.out.println("starting at = " + (char)colOrigin + (char)rowOrigin);
	}

	// colInt and rowInt are the letters to build to return
	int colInt = colOrigin + setColumn - 1;
	int rowInt = rowOrigin + setRow;
	boolean rollover = false;

	if (colInt > Z) {
	    colInt = colInt - Z + A - 1;
	    rollover = true;
	    if (DEBUG) System.out.println("rolling over col, new value: " + (char)colInt);
	}

	if (colInt == I || (colOrigin < I && colInt > I) || 
 	    ((colInt > I || colOrigin < I) && rollover)) {
	    colInt++;
	    if (DEBUG) System.out.println("skipping I in col, new value: " + (char)colInt);
	}
	if (colInt == O || (colOrigin < O && colInt > O) || 
 	    ((colInt > O || colOrigin < O) && rollover)) {
	    colInt++;
	    if (DEBUG) System.out.println("skipping O in col, new value: " + (char)colInt);
	    if (colInt == I) {
		colInt++;
		if (DEBUG) System.out.println("  hit I, new value: " + (char)colInt);
	    }
	}

	if (colInt > Z) {
	    colInt = colInt - Z + A - 1;
	    if (DEBUG) System.out.println("rolling(2) col, new value: " + (char)rowInt);
	}

	if (rowInt > V) {
	    rowInt = rowInt - V + A - 1;
	    rollover = true;
	    if (DEBUG) System.out.println("rolling over row, new value: " + (char)rowInt);
	} else {
	    rollover = false;
	}

	if (rowInt == I || (rowOrigin < I && rowInt > I) || 
 	    ((rowInt > I || rowOrigin < I) && rollover)) {
	    rowInt++;
	    if (DEBUG) System.out.println("skipping I in row, new value: " + (char)rowInt);
	}

	if (rowInt == O || (rowOrigin < O && rowInt > O) || 
 	    ((rowInt > O || rowOrigin < O) && rollover)) {
	    rowInt++;
	    if (DEBUG) System.out.println("skipping O in row, new value: " + (char)rowInt);
	    if (rowInt == I) {
		rowInt++;
		if (DEBUG) System.out.println("  hit I, new value: " + (char)rowInt);
	    }
	}

	if (rowInt > V) {
	    rowInt = rowInt - V + A - 1;
	    if (DEBUG) System.out.println("rolling(2) row, new value: " + (char)rowInt);
	}

	String twoLetter = (char)colInt + "" + (char)rowInt;

	if (DEBUG) {
	    System.out.println("ending at = " + twoLetter);
	}

	return twoLetter;
    }

    /**
     * Testing method, used to print out the MGRS 100k two letter set tables.
     */
    protected void print100kSets() {
	StringBuffer sb = null;
	for (int set = 1; set <= 6; set++) {
	    System.out.println("-------------\nFor 100K Set " + set + ":\n-------------\n");
	    for (int i = 19; i >=0; i -= 1) {
		sb = new StringBuffer((i*100000) + "\t| ");

		for (int j = 1; j <= 8; j++) {
 		    sb.append(" " + get100kID(j, i, set));
		}

		sb.append(" |");
		System.out.println(sb);
	    }
	}
    }

    public static OMGraphic getRectangle(LatLonPoint llp, int accuracy) {
	MGRSPoint mgrs = new MGRSPoint();
	mgrs.setAccuracy(accuracy);
	LLtoMGRS(llp, Ellipsoid.WGS_84, mgrs);

	Debug.output("------\n  Original - " + mgrs.getMGRS() + 
		     ", with easting " + mgrs.easting + 
		     ", northing " + mgrs.northing);

	mgrs = new MGRSPoint(mgrs.getMGRS());

	Debug.output("  corner point - " + mgrs.getMGRS() + 
		     ", with easting " + mgrs.easting + 
		     ", northing " + mgrs.northing);

	LatLonPoint llp1 = mgrs.toLatLonPoint();

	float accuracyBonus = 100000f/(float)Math.pow(10, accuracy);
	Debug.output("adding " + accuracyBonus + " meters with accuracy");

	LatLonPoint llp2 = UTMtoLL(Ellipsoid.WGS_84,
				   mgrs.northing, 
				   mgrs.easting + accuracyBonus,
				   mgrs.zone_number, mgrs.zone_letter, null);

	LatLonPoint llp3 = UTMtoLL(Ellipsoid.WGS_84,
				   mgrs.northing + accuracyBonus, 
				   mgrs.easting + accuracyBonus,
				   mgrs.zone_number, mgrs.zone_letter, null);

	LatLonPoint llp4 = UTMtoLL(Ellipsoid.WGS_84,
				   mgrs.northing + accuracyBonus, 
				   mgrs.easting,
				   mgrs.zone_number, mgrs.zone_letter, null);

	float[] llpoints = new float[10];
	llpoints[0] = llp1.getLatitude();
	llpoints[1] = llp1.getLongitude();
	llpoints[2] = llp2.getLatitude();
	llpoints[3] = llp2.getLongitude();
	llpoints[4] = llp3.getLatitude();
	llpoints[5] = llp3.getLongitude();
	llpoints[6] = llp4.getLatitude();
	llpoints[7] = llp4.getLongitude();
	llpoints[8] = llp1.getLatitude();
	llpoints[9] = llp1.getLongitude();

	OMGraphicList list = new OMGraphicList();

	OMPoly poly = new OMPoly(llpoints, OMGraphic.DECIMAL_DEGREES,
				 OMGraphic.LINETYPE_GREATCIRCLE);
	poly.setLinePaint(java.awt.Color.red);
	list.add(poly);

	OMPoint pt =new OMPoint(llp.getLatitude(), llp.getLongitude());
	pt.setLinePaint(java.awt.Color.green);
	list.add(pt);

	return list;
    }

    public static void main(String[] argv) {
	Debug.init();

	ArgParser ap = new ArgParser("MGRSPoint");
	ap.add("mgrs", "Print Latitude and Longitude for MGRS value", 1);
	ap.add("latlon", "Print MGRS for Latitude and Longitude values", 2, true);
	ap.add("sets", "Print the MGRS 100k table");
	ap.add("altsets", "Print the MGRS 100k table for the Bessel ellipsoid");

	if (!ap.parse(argv)) {
	    ap.printUsage();
	    System.exit(0);
	}

	String arg[];
	arg = ap.getArgValues("sets");
	if (arg != null) {
	    new MGRSPoint().print100kSets();
	}
	
	arg = ap.getArgValues("altsets");
	if (arg != null) {
	    MGRSPoint mgrsp = new MGRSPoint();
	    mgrsp.setOriginColumnLetters(BESSEL_SET_ORIGIN_COLUMN_LETTERS);
	    mgrsp.setOriginRowLetters(BESSEL_SET_ORIGIN_ROW_LETTERS);
	    mgrsp.print100kSets();
	}
	
	arg = ap.getArgValues("mgrs");
	if (arg != null) {
	    try {
		MGRSPoint mgrsp = new MGRSPoint(arg[0]);
		Debug.output(arg[0] + " is " + mgrsp.toLatLonPoint());	
	    } catch (NumberFormatException nfe) {
		Debug.error(nfe.getMessage());
	    }
	}
	
	arg = ap.getArgValues("latlon");
	if (arg != null) {
	    try {

		float lat = Float.parseFloat(arg[0]);
		float lon = Float.parseFloat(arg[1]);

		LatLonPoint llp = new LatLonPoint(lat, lon);
		MGRSPoint mgrsp = LLtoMGRS(llp);

		Debug.output(llp + " is " + mgrsp);

	    } catch (NumberFormatException nfe) {
		Debug.error("The numbers provided:  " + argv[0] + ", " +
			    argv[1] + " aren't valid");
	    }
	}

    }
}
