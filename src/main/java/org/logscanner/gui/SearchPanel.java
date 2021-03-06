package org.logscanner.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;
import org.logscanner.common.gui.DisabledPanel;
import org.logscanner.common.gui.ListItem;
import org.logscanner.common.gui.datepicker.DatePickerSettings;
import org.logscanner.common.gui.datepicker.DateTimePicker;
import org.logscanner.common.gui.datepicker.TimePickerSettings;
import org.logscanner.data.LocationGroup;
import org.logscanner.jobs.CopyFilesWriter;
import org.logscanner.service.AppProperties;
import org.logscanner.service.JobResultModel;
import org.logscanner.service.LocationDao;
import org.logscanner.service.LogPatternDao;
import org.logscanner.service.SearchModel;
import org.logscanner.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.adapter.RadioButtonAdapter;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Victor Kadachigov
 */
@Component
public class SearchPanel extends JPanel 
{
	private static final long serialVersionUID = 1L;
	private static final int HPAD = 5; 
	private static final int VPAD = 5; 

	@Autowired
	private SearchModel searchModel;
	@Autowired
	private JobResultModel resultModel;
	@Autowired
	private SelectSaveToFileAction selectSaveToFileAction;
	@Autowired
	private SelectSaveToFolderAction selectSaveToFolderAction;
	@Autowired
	private SearchAction searchStartAction;
	@Autowired
	private SelectLocationsAction selectLocationsAction;
	@Autowired
	private LogPatternDao patternDao;
	@Autowired
	private MessageSourceAccessor messageAccessor;
	
	private DateTimePicker fromPicker;
	private DateTimePicker toPicker;
	private JTextField locationsText;
	private JCheckBox saveToCheck;
	private JRadioButton saveTypeRadioButton1; 
	private JRadioButton saveTypeRadioButton2;
	private JTextField saveToFileText;
	private JTextField saveToFolderText;
	private JTextField searchText;
	private BeanAdapter<SearchModel> beanAdapter;

	@PostConstruct
	public void init()
	{
		setLayout(new BorderLayout());
		
		beanAdapter = new BeanAdapter<>(searchModel, true);
		
		final JPanel panel1 = new JPanel();
		panel1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
		
		fromPicker = new DateTimePicker(createDatePickerSettings(), createTimePickerSettings());
		ValueModel fromDateAdapter = beanAdapter.getValueModel("fromDate");
		Bindings.bind(fromPicker.getDatePicker(), "date", fromDateAdapter);
		ValueModel fromTimeAdapter = beanAdapter.getValueModel("fromTime");
		Bindings.bind(fromPicker.getTimePicker(), "time", fromTimeAdapter);

		final Box box1 = Box.createHorizontalBox();
		box1.add(new JLabel(messageAccessor.getMessage("search_panel.text.from")));
		box1.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box1.add(fromPicker);

		toPicker = new DateTimePicker(createDatePickerSettings(), createTimePickerSettings());
		ValueModel toDateAdapter = beanAdapter.getValueModel("toDate");
		Bindings.bind(toPicker.getDatePicker(), "date", toDateAdapter);
		ValueModel toTimeAdapter = beanAdapter.getValueModel("toTime");
		Bindings.bind(toPicker.getTimePicker(), "time", toTimeAdapter);
		box1.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box1.add(new JLabel(messageAccessor.getMessage("search_panel.text.to")));
		box1.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box1.add(toPicker);
		box1.add(Box.createHorizontalGlue());
		
		panel1.add(box1);
		panel1.add(Box.createRigidArea(new Dimension(0, VPAD)));

		locationsText = new JTextField();
		locationsText.setEditable(false);
		final Box box2 = Box.createHorizontalBox();
		box2.add(new JLabel(messageAccessor.getMessage("search_panel.text.where")));
		box2.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box2.add(locationsText);
//		selectLocationsAction.addPropertyChangeListener(
//				(PropertyChangeEvent event) -> 
//				{
//					if (Objects.equals(event.getPropertyName(), "locations"))
//						locationsText.setText(createLocationsString());
//				}
//		);
		searchModel.addPropertyChangeListener(
				"selectedLocations",
				(PropertyChangeEvent event) -> {
					locationsText.setText(createLocationsString((Set<String>)event.getNewValue()));
				}
		);
		box2.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box2.add(GuiHelper.createToolBarButton(selectLocationsAction));
		box2.add(Box.createHorizontalGlue());
		panel1.add(box2);
		panel1.add(Box.createRigidArea(new Dimension(0, VPAD)));
		
		final Box box6 = Box.createHorizontalBox();
		box6.add(new JLabel(messageAccessor.getMessage("search_panel.text.file_mask")));
		box6.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		
		List<ListItem<String>> patterns = createPatternsList();
		JComboBox<ListItem<String>> patternCombo = new JComboBox<>(patterns.toArray(new ListItem[patterns.size()]));
		patternCombo.addItemListener(
				(ItemEvent event) -> 
				{
					if (event.getStateChange() == ItemEvent.SELECTED)
						searchModel.setPatternCode(((ListItem<String>)event.getItem()).getValue());
				}
		);
		if (searchModel.getPatternCode() != null)
		{
			Optional<ListItem<String>> op = patterns.stream()
						.filter(p -> Objects.equals(p.getValue(), searchModel.getPatternCode()))
						.findFirst();
			if (op.isPresent())
				patternCombo.setSelectedItem(op.get());
			else
				searchModel.setPatternCode(null);
		}
		if (searchModel.getPatternCode() == null)
		{
			patternCombo.setSelectedIndex(0);
			searchModel.setPatternCode(patterns.get(0).getValue());
		}
			
		box6.add(patternCombo);
		box6.add(Box.createHorizontalGlue());
		panel1.add(box6);
		panel1.add(Box.createRigidArea(new Dimension(0, VPAD)));
		
		final Box box3 = Box.createHorizontalBox();
		ValueModel saveToFileCheckAdapter = beanAdapter.getValueModel("saveResults");
		saveToCheck = BasicComponentFactory.createCheckBox(saveToFileCheckAdapter, null);
		saveToCheck.addItemListener(
				(ItemEvent event) -> {
					enableSaveToFileElements();
				}
		);
		saveToCheck.setText(messageAccessor.getMessage("search_panel.text.result"));
		box3.add(saveToCheck);

		ValueModel saveTypeRadioButtonAdapter = beanAdapter.getValueModel("saveType");
		saveTypeRadioButton1 = BasicComponentFactory.createRadioButton(
													saveTypeRadioButtonAdapter, 
													SearchModel.SAVE_TYPE_FILE, 
													messageAccessor.getMessage("search_panel.text.to_file")
											);
		saveTypeRadioButton1.addItemListener(
				(ItemEvent event) -> {
					enableSaveToFileElements();
				}
		);
		saveTypeRadioButton2 = BasicComponentFactory.createRadioButton(
													saveTypeRadioButtonAdapter, 
													SearchModel.SAVE_TYPE_FOLDER, 
													messageAccessor.getMessage("search_panel.text.to_folder")
											);
		saveTypeRadioButton2.addItemListener(
				(ItemEvent event) -> {
					enableSaveToFileElements();
				}
		);
		box3.add(saveTypeRadioButton1);
		
		ValueModel saveToFileTextAdapter = beanAdapter.getValueModel("resultFile");
		saveToFileText = BasicComponentFactory.createTextField(saveToFileTextAdapter, false);
		box3.add(saveToFileText);
		box3.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box3.add(GuiHelper.createToolBarButton(selectSaveToFileAction));
		box3.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		
		box3.add(saveTypeRadioButton2);
		ValueModel saveToFolderTextAdapter = beanAdapter.getValueModel("resultFolder");
		saveToFolderText = BasicComponentFactory.createTextField(saveToFolderTextAdapter, false);
		box3.add(saveToFolderText);
		box3.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		box3.add(GuiHelper.createToolBarButton(selectSaveToFolderAction));
		
		box3.add(Box.createHorizontalGlue());
		panel1.add(box3);
		panel1.add(Box.createRigidArea(new Dimension(0, VPAD)));

		final JPanel panel2 = new JPanel(new BorderLayout());
		panel2.add(new JLabel(messageAccessor.getMessage("search_panel.text.search")), BorderLayout.NORTH);
		panel2.add(createStringsSearchPanel(), BorderLayout.CENTER);
//		JTabbedPane tabbedPane = new JTabbedPane();
//		panel2.add(tabbedPane, BorderLayout.CENTER);
//		
//		tabbedPane.addTab("Строка", createStringsSearchPanel());
//		tabbedPane.addTab("RqUID", createRquidSearchPanel());
//		tabbedPane.addTab("Счёт", createAccountSearchPanel());
		
		panel1.add(panel2);

		final DisabledPanel disabledPanel = new DisabledPanel(panel1);
		add(disabledPanel, BorderLayout.NORTH);

		final Box box5 = Box.createVerticalBox();
		final Box box4 = Box.createHorizontalBox();
		final JButton searchButton = new JButton(searchStartAction);
		resultModel.addPropertyChangeListener(
				"jobState",
				(PropertyChangeEvent event) -> 
				{
					if (event.getNewValue() == JobResultModel.JobState.RUNNED)
					{
						disabledPanel.setEnabled(false);
						searchButton.setText(messageAccessor.getMessage("action.search.stop"));
						searchButton.setEnabled(true);
					}
					else if (event.getNewValue() == JobResultModel.JobState.STOPPING)
					{
						searchButton.setEnabled(false);
					}
					else if (event.getNewValue() == JobResultModel.JobState.STOPPED)
					{
						disabledPanel.setEnabled(true);
						searchButton.setText(messageAccessor.getMessage("action.search.title"));
						searchButton.setEnabled(true);
					}
				}
		);
		box4.add(searchButton);
		box4.add(Box.createHorizontalGlue());
		
		box5.add(box4);
		box5.add(Box.createRigidArea(new Dimension(0, VPAD * 2)));
		
		add(box5, BorderLayout.SOUTH);
		
		enableSaveToFileElements();
	}
	
	private List<ListItem<String>> createPatternsList()
	{
		List<ListItem<String>> result = new ArrayList<>();
		patternDao.getAll().forEach(
				p -> result.add(new ListItem<String>(p.getCode(), p.getDescription()))
		);
		return result;
	}
	
	private void enableSaveToFileElements()
	{
		boolean enable = searchModel.isSaveResults();
		boolean enable1 = enable && searchModel.getSaveType() == SearchModel.SAVE_TYPE_FILE;
		boolean enable2 = enable && searchModel.getSaveType() == SearchModel.SAVE_TYPE_FOLDER;
		saveTypeRadioButton1.setEnabled(enable);
		saveTypeRadioButton2.setEnabled(enable);
		saveToFileText.setEnabled(enable1);
		selectSaveToFileAction.setEnabled(enable1);
		saveToFolderText.setEnabled(enable2);
		selectSaveToFolderAction.setEnabled(enable2);
	}
	
	private JPanel createStringsSearchPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		final Box box1 = Box.createHorizontalBox();
		box1.add(new JLabel(messageAccessor.getMessage("search_panel.text.text")));
		box1.add(Box.createRigidArea(new Dimension(HPAD, 0)));
		ValueModel searchTextAdapter = beanAdapter.getValueModel("searchString");
		searchText = BasicComponentFactory.createTextField(searchTextAdapter, false);
		box1.add(searchText);
		panel.add(box1);
		panel.add(Box.createRigidArea(new Dimension(0, VPAD)));
		
//		final Box box2 = Box.createHorizontalBox();
//		box2.add(new JLabel("кодировка:"));
//		box2.add(Box.createRigidArea(new Dimension(HPAD, 0)));
//		ValueModel encodingPropertyAdapter = beanAdapter.getValueModel("encoding");
//		ComboBoxAdapter adapter = new ComboBoxAdapter(props.getEncodings(), encodingPropertyAdapter); 
//		JComboBox encodingCombo = new JComboBox<>(adapter);
//		box2.add(encodingCombo);
//		box2.add(Box.createHorizontalGlue());
//		panel.add(box2);
		
		return panel;
	}
	
	private JPanel createRquidSearchPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("Ещё не готово", SwingConstants.CENTER), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createAccountSearchPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("Пока делаем", SwingConstants.CENTER), BorderLayout.CENTER);
		return panel;
	}
	
	private String createLocationsString(Set<String> selectedLocations)
	{
		StringBuilder sb = new StringBuilder();
		LocationDao locationDao = ServiceHelper.getBean(LocationDao.class);
		List<LocationGroup> groups = new ArrayList<>();
		List<String> groupNames = new ArrayList<>();
		List<String> locationNames = new ArrayList<>();
		Set<String> locationCodes = new HashSet<>();
		for (String code : selectedLocations)
		{
			LocationGroup group = locationDao.getGroupByCode(code);
			if (group != null)
			{
				groups.add(group);
				groupNames.add(group.getName());
			}
			else
				locationCodes.add(code);
		}
		for (String code : locationCodes)
		{
			LocationGroup parent = locationDao.getParent(code);
			if (!groups.contains(parent))
				locationNames.add(code);
		}
		
		if (!groupNames.isEmpty())
			sb.append(StringUtils.join(groupNames, ','));
		if (!locationNames.isEmpty())
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append(StringUtils.join(locationNames, ','));
		}

		return  sb.toString();
	}
	
	private DatePickerSettings createDatePickerSettings()
	{
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		DatePickerSettings dps = new DatePickerSettings(); 
		dps.setFirstDayOfWeek(DayOfWeek.MONDAY);
		dps.setFormatForDatesCommonEra(dateFormatter);
		dps.setFormatForTodayButton(dateFormatter);
		dps.setGapBeforeButtonPixels(5);
		return dps;
	}
	
	private TimePickerSettings createTimePickerSettings()
	{
		TimePickerSettings tps = new TimePickerSettings();
		tps.setFormatForDisplayTime("HH:mm:ss");
		tps.setFormatForMenuTimes("HH:mm");
		tps.setGapBeforeButtonPixels(5);
		return tps;
	}
	
}
