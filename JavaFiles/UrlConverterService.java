package com.milky.trackerWeb.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milky.trackerWeb.model.Competitor;
import com.milky.trackerWeb.repository.CompetitorDb;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.io.File;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

@Service
public class UrlConverterService {
	private WebDriver driver;

    @PostConstruct
    public void init() {
    	System.setProperty("webdriver.chrome.driver", "C:\\Users\\toadh\\chromedriver-win64\\chromedriver.exe");
    	driver = new ChromeDriver();
        processUrls();
    }
    
    @PreDestroy
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Autowired
    private CompetitorDb competitorDb;

    public void processUrls() {
    	ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, String> statusMap = new HashMap<>();
        HashMap<String, String> urlMap = new HashMap<>();
        try {
            // Read status from JSON file
            statusMap = objectMapper.readValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), new TypeReference<HashMap<String, String>>() {});
        } catch (IOException e) {
        	System.out.println("Could not read status.json. Initializing an empty status map.");
            e.printStackTrace();
            statusMap = new HashMap<>();  // Initialize an empty map if the file is empty or not found
        }
        //System.setProperty("webdriver.chrome.driver", "C:\\Users\\toadh\\chromedriver-win64\\chromedriver.exe");
        //WebDriver driver = new ChromeDriver();
        try {
            // Read status from JSON file
        	urlMap = objectMapper.readValue(new File("C:\\Users\\toadh\\Downloads\\Ebay.json"), new TypeReference<HashMap<String, String>>() {});
        } catch (IOException e) {
        	System.out.println("Could not read Ebay.json. Initializing an empty status map.");
            e.printStackTrace();
            urlMap = new HashMap<>();  // Initialize an empty map if the file is empty or not found
        }
        System.out.println("Ebay : "+urlMap.size()+"\nStatusMap : "+statusMap.size());
        try {
            int counter=0;
            for (String sourceName : urlMap.keySet()) {
            	counter++;
            	if(counter%100==0) System.out.println(counter);
            	if (statusMap.containsKey(sourceName) && ("SUCCESS".equals(statusMap.get(sourceName))|| "TERMINATE".equals(statusMap.get(sourceName)))) {
                    // Skip this iteration if the sourceName has already succeeded
                    continue;
                }
                Competitor competitor = new Competitor();
                String url = urlMap.get(sourceName);
                competitor.setSourcename(sourceName);
                competitor.setUrl(url);
                //System.out.println("Navigating to URL: " + url);
                driver.get(url);
                try {
                    Wait<WebDriver> wait = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class);

                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(.,'See All')]")));
                    String convertedUrl = element.getAttribute("href");

                    competitor.setConvertedUrl(convertedUrl);
                    competitor.setStatus(Competitor.Status.SUCCESS);
                    statusMap.put(sourceName, "SUCCESS");
                    objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                    competitorDb.save(competitor);
                    System.out.println("Initial success: "+sourceName+" : "+url);
                    continue;
                } catch (Exception e) {
                    //System.out.println("Try other checks!!");
                }
                try {
                    WebElement pageNoticeElement = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='page-notice__main']")));
                    
                    String pageNoticeText = pageNoticeElement.getText();

                    if ("No longer a registered user.".equals(pageNoticeText) || "Sorry, this user was not found.".equals(pageNoticeText)||pageNoticeText.contains("The seller is away until")) {
                        statusMap.put(sourceName, "TERMINATE");
                        objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                        continue;  // Skip the rest of this iteration
                    }
                } catch (TimeoutException | NoSuchElementException e) {
                    //System.out.println("Page notice not found. Proceeding to next step.");
                }
                try {
                    WebElement aboutElement = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h2[@class='str-title']")));

                    String aboutText = aboutElement.getText();

                    if ("About".equals(aboutText)) {
                        // Add your logic here if the scraped value is "About"
                        System.out.println("About section found. Performing special logic.");
                        statusMap.put(sourceName, "TERMINATE");
                        objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                        continue;
                    }
                } catch (TimeoutException | NoSuchElementException e) {
                    //System.out.println("About section not found. Proceeding to next step.");
                }
                // Second Check
                try {
                    WebElement discoverStoresElement = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[@class='str-status__link']")));

                    String discoverStoresText = discoverStoresElement.getText();

                    if ("Discover stores".equals(discoverStoresText)) {
                        statusMap.put(sourceName, "TERMINATE");
                        objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                        continue;  // Skip the rest of this iteration
                    }
                } catch (TimeoutException | NoSuchElementException e) {
                    System.out.println("Discover stores link not found. Proceeding to next step.");
                }
                boolean firstCheckPassed = false;
                try {
                    WebElement shipToButton = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@data-testid='ship-to-button']")));
                    shipToButton.click();
                    
                    // Select 'Canada' from the dropdown that appears
                    WebElement canadaOption = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//option[contains(text(),'Canada')]")));
                    canadaOption.click();
                    
                    // Click the submit button
                    WebElement submitButton = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[contains(@class,'location-selection__form')]")));
                    submitButton.click();
                    firstCheckPassed = true;
                } catch (TimeoutException | NoSuchElementException e) {
                    //System.out.println("Initial checks Passed. Proceeding to next step.");
                }
                try {
                    Wait<WebDriver> wait = new FluentWait<>(driver)
                            .withTimeout(Duration.ofSeconds(3))
                            .pollingEvery(Duration.ofMillis(200))
                            .ignoring(NoSuchElementException.class);

                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(.,'See All')]")));
                    String convertedUrl = element.getAttribute("href");

                    competitor.setConvertedUrl(convertedUrl);
                    competitor.setStatus(Competitor.Status.SUCCESS);
                    statusMap.put(sourceName, "SUCCESS");
                    objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                    competitorDb.save(competitor);
                } catch (Exception e) {
                    //e.printStackTrace();
                    if (firstCheckPassed) {
                        statusMap.put(sourceName, "TERMINATE");  // Set status to 'TERMINATE' if first check was successful but second failed
                        objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                    } else {
                        statusMap.put(sourceName, "FAILED");
                        objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
                    }
                }
            }
            objectMapper.writeValue(new File("C:\\Users\\toadh\\Downloads\\status.json"), statusMap);
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}

