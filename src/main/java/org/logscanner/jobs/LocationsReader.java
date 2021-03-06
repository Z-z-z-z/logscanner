package org.logscanner.jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.logscanner.AppConstants;
import org.logscanner.data.Location;
import org.logscanner.data.LocationType;
import org.logscanner.service.LocationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Victor Kadachigov
 */
@Slf4j
public class LocationsReader implements ItemReader<Location>, StepExecutionListener
{
	private Object monitor = new Object();
	
	@Autowired
	private LocationDao locationDao;
	
	private StepExecution stepExecution;
	private List<Location> locations;

	public LocationsReader() 
	{
	}

	@Override
	public Location read() throws UnexpectedInputException, ParseException, NonTransientResourceException 
	{
		Location result = null;
		
		synchronized (monitor) 
		{
			if (locations == null)
				locations = initList();
			if (!locations.isEmpty()) 
				result = locations.remove(0);
		}
		
		if (result != null)
			log.info("read() {}", result);
		
		return result;
	}

	private List<Location> initList() throws UnexpectedInputException
	{
		List<Location> list = new ArrayList<>();
		List<String> paths = new ArrayList<>();
		String idString = stepExecution.getJobParameters().getString(AppConstants.JOB_PARAM_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(idString, ",");
		while (tokenizer.hasMoreTokens())
		{
			String id = StringUtils.trim(tokenizer.nextToken());
			Location l = locationDao.getByCode(id);
			if (l != null)
			{
				list.add(l);
				paths.add(l.getPath());
			}
			else
				log.warn("Location with id '{}' not found", id);
		}
		
		stepExecution.getExecutionContext().put(AppConstants.PROP_COMMON_PATH, getCommonPrefix(paths));
		
		return list;
	}
	
	private String getCommonPrefix(List<String> paths)
	{
//		String result = "";
//		String commonPath = StringUtils.getCommonPrefix(paths.toArray(new String[paths.size()]));
//		while (StringUtils.isNoneEmpty(commonPath))
//		{
//			File f = new File(commonPath);
//			if (f.exists() && f.isDirectory())
//			{
//				result = f.getAbsolutePath();
//				break;
//			}
//			else
//			{
//				int index = commonPath.lastIndexOf(File.separator);
//				if (index >= 0)
//				{
//					commonPath = commonPath.substring(0, index);
//					if (commonPath.equals("\\")) // windows net path \\...
//						commonPath = "";
//				}
//				else 
//					commonPath = "";
//			}
//		}
////		log.info("getCommonPrefix() = {}", result);
//		return result;
		
		return "";
	}

    @AfterStep
	@Override
	public ExitStatus afterStep(StepExecution stepExecution)
	{
    	locations = null;
		return null;
	}

	@BeforeStep
	@Override
	public void beforeStep(StepExecution stepExecution)
    {
        this.stepExecution = stepExecution;
    }
}
