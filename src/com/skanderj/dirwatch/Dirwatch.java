package com.skanderj.dirwatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.skanderj.ts4j.Task;
import com.skanderj.ts4j.TaskScheduler;
import com.skanderj.ts4j.TaskType;
import com.skanderj.ts4j.TimeValue;
import com.skanderj.yalt.LogLevel;
import com.skanderj.yalt.Logger;

public final class Dirwatch {
	private Dirwatch() {
		return;
	}

	public static void main(String[] args) {
		Logger.log(Dirwatch.class, LogLevel.INFO, "Dirwatch by Skander J. - Thanks for using my software!");
		if (args.length == 1) {
			String path = args[0];
			Logger.log(Dirwatch.class, LogLevel.INFO, "Inspecting path: %s", path);
			File directory = new File(path);
			if (directory.exists() && directory.isDirectory()) {
				Logger.log(Dirwatch.class, LogLevel.INFO, "Found an existing directory - path is valid!");
				TaskScheduler.scheduleTask(path + "-watcher",
						new Task(new TimeValue(0, TimeUnit.MILLISECONDS), new TimeValue(1000, TimeUnit.MILLISECONDS)) {
							FileProperties[] properties;

							@Override
							public TaskType type() {
								return TaskType.FIXED_DELAY;
							}

							@Override
							public void execute() {
								if (this.properties == null) {
									System.out.println();
									Logger.log(Dirwatch.class, LogLevel.INFO, "First scan...");
									List<FileProperties> propertiesAsList = new ArrayList<FileProperties>();
									for (File file : directory.listFiles()) {
										if (file.isFile()) {
											Logger.log(Dirwatch.class, LogLevel.INFO, "Adding file %s (%d)",
													file.getName(), file.length());
											propertiesAsList.add(new FileProperties(file.getName(), file.length()));
										}
									}
									this.properties = (FileProperties[]) propertiesAsList
											.toArray(new FileProperties[propertiesAsList.size()]);
								} else {
									List<FileProperties> propertiesAsList = new ArrayList<FileProperties>();
									for (File file : directory.listFiles()) {
										if (file.isFile()) {
											propertiesAsList.add(new FileProperties(file.getName(), file.length()));
										}
									}
									if (propertiesAsList.size() != this.properties.length) {
										System.out.println();
										Logger.log(Dirwatch.class, LogLevel.INFO,
												"The directory contents have been changed (%d vs %d)",
												propertiesAsList.size(), this.properties.length);
										if (propertiesAsList.size() > this.properties.length) {
											boolean foundIn = false;
											for (FileProperties props : propertiesAsList) {
												for (FileProperties props2 : this.properties) {
													if (props.name.equals(props2.name)) {
														foundIn = true;
														if (props.size != props2.size) {
															System.out.println();
															Logger.log(Dirwatch.class, LogLevel.INFO,
																	"File %s' size has changed, from %d to %d",
																	props.name, props2.size, props.size);
														}
													}
												}
												if (!foundIn) {
													Logger.log(Dirwatch.class, LogLevel.INFO,
															"A new file has been detected: %s (%d)", props.name,
															props.size);
												}
												foundIn = false;
											}
										} else {
											boolean foundIn = false;
											for (FileProperties props : this.properties) {
												for (FileProperties props2 : propertiesAsList) {
													if (props.name.equals(props2.name)) {
														foundIn = true;
														if (props.size != props2.size) {
															System.out.println();
															Logger.log(Dirwatch.class, LogLevel.INFO,
																	"File %s' size has changed, from %d to %d",
																	props.name, props2.size, props.size);
														}
													}
												}
												if (!foundIn) {
													Logger.log(Dirwatch.class, LogLevel.INFO,
															"A file has been deleted: %s (%d)", props.name,
															props.size);
												}
												foundIn = false;
											}

										}
									}
									this.properties = (FileProperties[]) propertiesAsList
											.toArray(new FileProperties[propertiesAsList.size()]);
								}
							}
						});
			} else {
				Logger.log(Dirwatch.class, LogLevel.SEVERE, "The provided path either points to a non-existing directory or to a file, please specifiy a valid path");
				System.exit(0);
			}
		} else {
			Logger.log(Dirwatch.class, LogLevel.SEVERE, "Usage: java Dirwatch [path]");
			System.exit(-1);
		}
	}
}
