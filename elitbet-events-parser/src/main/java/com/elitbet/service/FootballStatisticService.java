package com.elitbet.service;

import com.elitbet.model.FootballStatistic;
import com.elitbet.model.EventWrapper;
import com.elitbet.model.TournamentWrapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class FootballStatisticService extends FootballService {

    @Override
    public void loadElements(WebDriver driver) {
        loadTournaments(driver);
    }

    private void loadTournaments(WebDriver driver){
        WebElement date;
        try {
            date = loadElement(driver, By.className("today"));
        } catch (Exception e) {
            System.out.println("Today web element not loaded");
            return;
        }
        List<WebElement> tournaments = driver.findElements(By.xpath("//div/table"));
        LocalTime now = LocalTime.now();
        List<TournamentWrapper> tournamentWrappers = new LinkedList<>();
        for(WebElement tournament:tournaments){
            TournamentWrapper wrapper = new TournamentWrapper(tournament, date, now);
            tournamentWrappers.add(wrapper);
        }
        runTournamentExecutorService(tournamentWrappers.subList(0,50));
    }

    private void runTournamentExecutorService(List<TournamentWrapper> tournamentWrappers){
        ExecutorService executorService = Executors.newFixedThreadPool(tournamentWrappers.size());
        List<Callable<Void>> creators = new ArrayList<>();
        Queue<EventWrapper> eventWrappers = new ConcurrentLinkedQueue<>();
        for(TournamentWrapper tournamentWrapper:tournamentWrappers){
            creators.add(() -> {
                WebElement date = tournamentWrapper.getDate();
                WebElement tournamentElement = tournamentWrapper.getTournament();
                WebElement country = tournamentElement.findElement(By.className("country_part"));
                WebElement tournament = tournamentElement.findElement(By.className("tournament_part"));
                List<WebElement> eventElements = tournamentElement.findElements(By.xpath("tbody/tr"));
                LocalTime update = tournamentWrapper.getUpdate();
                for(WebElement eventElement:eventElements){
                    eventWrappers.add(new EventWrapper(tournament,date, country, eventElement, update));
                }
                return null;
            });
        }
        try {
            executorService.invokeAll(creators);
            runEventExecutorService(eventWrappers);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void runEventExecutorService(Queue<EventWrapper> eventWrappers){
        ExecutorService executorService = Executors.newFixedThreadPool(eventWrappers.size());
        List<Callable<Void>> creators = new ArrayList<>();
        for (EventWrapper wrapper : eventWrappers) {
            creators.add(() -> {
                FootballStatistic statistic = getStatistic(wrapper);
                urls.add(statistic.toURL());
                return null;
            });
        }
        try {
            executorService.invokeAll(creators);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private FootballStatistic getStatistic(EventWrapper wrapper){

        FootballStatistic statistic = new FootballStatistic();
        statistic.setId(wrapper.getEvent().getAttribute("id"));
        statistic.setDate(wrapper.getDate().getText());
        statistic.setTournament(wrapper.getTournament().getText());
        statistic.setCountry(wrapper.getCountry().getText().replace(":",""));
        statistic.setStartTime(wrapper.getEvent().findElement(By.cssSelector("td.time.cell_ad")).getText());
        statistic.setStatus(wrapper.getEvent().findElement(By.cssSelector("td.timer.cell_aa")).getText());
        statistic.setHomeTeamName(wrapper.getEvent().findElement(By.cssSelector("td.team-home.cell_ab")).getText());
        statistic.setAwayTeamName(wrapper.getEvent().findElement(By.cssSelector("td.team-away.cell_ac")).getText());

        int[] score = parseScore(wrapper.getEvent().findElement(By.cssSelector("td.score.cell_sa")).getText());
        statistic.setHomeTeamGoals(score[0]);
        statistic.setAwayTeamGoals(score[1]);

        int[] firstHalfScore = parseScore(wrapper.getEvent().findElement(By.cssSelector("td.part-top.cell_sb")).getText());
        statistic.setHomeTeamFirstHalfGoals(firstHalfScore[0]);
        statistic.setAwayTeamFirstHalfGoals(firstHalfScore[1]);

        statistic.setLastUpdated(wrapper.getUpdate());

        return statistic;
    }

    private int[] parseScore(String scoreString){
        int indexLeft = scoreString.indexOf("(");
        int indexRight = scoreString.indexOf(")");
        if(indexLeft>0){
            scoreString = scoreString.substring(indexLeft+1, indexRight-1);
        }
        int[] score = new int[2];
        try {
            String[] scoreArray = scoreString.split("-");
            score[0] = Integer.valueOf(scoreArray[0].trim());
            score[1] = Integer.valueOf(scoreArray[1].trim());
        } catch (Exception e){
            score[0] = 0;
        }
        return score;
    }

}