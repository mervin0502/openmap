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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/gui/Attic/LoadPropertiesMenuItem.java,v $
// $RCSfile: LoadPropertiesMenuItem.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.gui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.beans.beancontext.*;
import java.util.*;
import java.io.*;

import com.bbn.openmap.*;
import com.bbn.openmap.util.Debug;

/**
 * This is a menu item that loads a file.  This file should be a valid
 * openmap properties file, with openmap.layers set and projection
 * properties set.  The components list is ignored, since it is pretty
 * dependent on order, and it's hard to write a good properties file
 * automatically with a valid components list.  <P> If you want to
 * change the components in an application, launch OpenMap with the
 * properties file you want.
 */
public class LoadPropertiesMenuItem extends JMenuItem 
    implements ActionListener {

    MapHandler mapHandler = null;

    public LoadPropertiesMenuItem() {
	super("Load Map...");
	addActionListener(this);
    }

    public void actionPerformed(ActionEvent ae) {
	//Collect properties
	if (mapHandler == null) {
	    return;
	}

	PropertyHandler ph = null;
	Iterator it = mapHandler.iterator();
	while (it.hasNext()) {
	    Object someObj = it.next();
	    if (someObj instanceof PropertyHandler) {
		ph = (PropertyHandler) someObj;
		break;
	    }
	}

	if (ph == null) {
	    Debug.error("Couldn't find PropertyHandler");
	    return;
	}


	FileDialog fd = new FileDialog(new Frame(), "Loading the map from a Properties file...", FileDialog.LOAD);
	fd.show();

	String fileName = fd.getFile();
	String dirName = fd.getDirectory();

	if (fileName == null) {
	    Debug.message("loadpropertiesmenuitem",
			  "User did not select any file");
	    return;
	}

	Debug.message("loadpropertiesmenuitem",
		      "User selected file " + dirName + File.separator +
		      fileName);

	File file = new File(new File(dirName), fileName);

	try {
	    Properties newProps = new Properties();
	    FileInputStream fis = new FileInputStream(file);

	    newProps.load(fis);

	    String test = newProps.getProperty("openmap." + LayerHandler.layersProperty);
	    if (test == null) {
		throw new IOException("Doesn't seem like a valid properties file");
	    }

	    // Just reset the projection and layers, not the components.
	    ph.loadProjectionAndLayers(mapHandler, newProps);

	} catch (FileNotFoundException fnfe) {
	    Debug.error(fnfe.getMessage());
	} catch (IOException ioe) {
	    InformationDelegator id = (InformationDelegator)mapHandler.get("com.bbn.openmap.InformationDelegator");

	    if (id != null) {
		id.displayMessage("Error loading file...",
				  "Error occured loading " + file.getAbsolutePath() + "\n" + ioe.getMessage());
	    }
	    Debug.error("Error occured loading " + file.getAbsolutePath());
	}
    }

    public void setMapHandler(MapHandler in_mapHandler) {
	
	Debug.message("loadpropertiesmenuitem",
		      "Setting mapHandler " + in_mapHandler);
	mapHandler = in_mapHandler;
    }
}