package org.logscanner.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;

import org.logscanner.Resources;
import org.logscanner.common.gui.BaseAction;
import org.logscanner.service.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Victor Kadachigov
 */
@Component
public class PreferencesAction extends BaseAction 
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(PreferencesAction.class);

	@Autowired
	private AppProperties props;

	public PreferencesAction() 
	{
		super(Resources.getStr("action.preferences.title"));
	}

	@Override
	protected void actionPerformed0(ActionEvent event) throws Exception 
	{
		if (!Desktop.isDesktopSupported())
			throw new UnsupportedOperationException("Can't do this. Desktop is not supported");
		
		Desktop desktop = Desktop.getDesktop();
		if (!desktop.isSupported(Desktop.Action.OPEN))
				throw new UnsupportedOperationException("Can't do open operation");
		
		desktop.open(props.getPreferencesDir().toFile());
	}
}
