package org.logscanner.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.logscanner.AppConstants;
import org.logscanner.Resources;
import org.logscanner.data.FileData;
import org.logscanner.logger.Logged;
import org.logscanner.logger.Logged.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * using ZipFileSystem. ZipFileSystem has in-memory implementation!!! To heavy
 * @author Victor Kadachigov
 */
@Slf4j
public class PackFilesWriter2 extends AbstractItemStreamItemWriter<FileData> implements StepExecutionListener
{
	private StepExecution stepExecution;
	private FileSystem zipFile;
	private Set<String> files;

	@Override
	@Logged(level = Level.DEBUG)
	public void write(List<? extends FileData> items) throws Exception 
	{
		for (FileData fileData : items)
		{
			String zipPath = fileData.getZipPath();
			if (files.contains(zipPath))
			{
				log.warn("File {} is already in archive. Skipping", zipPath);
				continue;
			}
			files.add(zipPath);
			log.info("Saving {} to {}", fileData.getFilePath(), zipPath);

			try (InputStream inputStream = fileData.getContentReader().getInputStream())
			{
				Path p = zipFile.getPath(zipPath);
				Files.createDirectories(p.getParent());
				try (OutputStream outputStream = Files.newOutputStream(p, StandardOpenOption.CREATE))
				{
					IOUtils.copy(inputStream, outputStream);
				}
			}
		}
	}
	
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException 
	{
		if (!isInitialized()) 
		{
			files = new ConcurrentSkipListSet<>();
			if (stepExecution.getJobParameters().getLong(AppConstants.JOB_PARAM_SAVE_TO_ARCHIVE) == 1L)
			{
				String resultFile = stepExecution.getJobParameters().getString(AppConstants.JOB_PARAM_OUTPUT_ARCHIVE_NAME);
				File file = new File(resultFile);

				log.info("Result file {}", file.getAbsolutePath());
				
		        try
				{
		        	if (file.exists())				
		        		file.delete();

					Map<String, String> env = new HashMap<>();
					env.put("create", "true");
					env.put("useTempFile", "true"); // seems that it doesn't work
					URI zipURI = new URI("jar:file", null, file.toURI().getPath(), null);
					zipFile = FileSystems.newFileSystem(zipURI, env);
				}
				catch (IOException | URISyntaxException ex)
				{
					throw new ItemStreamException(ex.getMessage(), ex);
				}
			}
			else
				throw new NotImplementedException(Resources.getStr("error.not_implemented"));
		}
	}

	@Override
	public void close() 
	{
		try 
		{
			if (isInitialized())
				zipFile.close();
		} 
		catch (IOException ex) 
		{
			throw new ItemStreamException(ex.getMessage(), ex);
		}
		finally 
		{
			zipFile = null;
			files = null;
		}
	}

    private boolean isInitialized() 
    {
		return zipFile != null;
	}

    @AfterStep
	@Override
	public ExitStatus afterStep(StepExecution stepExecution)
	{
		return null;
	}

	@BeforeStep
	@Override
	public void beforeStep(StepExecution stepExecution)
	{
        this.stepExecution = stepExecution;
	}
}
