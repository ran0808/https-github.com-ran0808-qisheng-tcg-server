package com.play.service;

import com.play.network.gameSet.card.Card;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CardService {
    @Autowired
    @Qualifier("cardLibrary")
    @Getter
    private Map<Integer, Card> cardLibrary;
    public Card getCardByName(String name){
        for (Card card : cardLibrary.values()) {
            if (card.getName().equals(name))
                return card;
        }
        return null;
    }
}
