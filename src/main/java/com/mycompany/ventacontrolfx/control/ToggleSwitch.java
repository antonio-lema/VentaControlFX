package com.mycompany.ventacontrolfx.control;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ToggleSwitch extends Parent {

    private BooleanProperty switchedOn = new SimpleBooleanProperty(false);

    private TranslateTransition translateAnimation = new TranslateTransition(Duration.seconds(0.25));
    private FillTransition fillAnimation = new FillTransition(Duration.seconds(0.25));
    private ParallelTransition animation = new ParallelTransition(translateAnimation, fillAnimation);

    public BooleanProperty switchedOnProperty() {
        return switchedOn;
    }

    public void setSwitchedOn(boolean on) {
        switchedOn.set(on);
    }

    public boolean isSwitchedOn() {
        return switchedOn.get();
    }

    public ToggleSwitch() {
        Rectangle background = new Rectangle(40, 20);
        background.setArcWidth(20);
        background.setArcHeight(20);
        background.setFill(Color.WHITE);
        background.setStroke(Color.LIGHTGRAY);

        Circle trigger = new Circle(10);
        trigger.setCenterX(10);
        trigger.setCenterY(10);
        trigger.setFill(Color.WHITE);
        trigger.setStroke(Color.LIGHTGRAY);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        trigger.setEffect(shadow);

        translateAnimation.setNode(trigger);
        fillAnimation.setShape(background);

        getChildren().addAll(background, trigger);

        switchedOn.addListener((obs, oldState, newState) -> {
            boolean isOn = newState.booleanValue();
            translateAnimation.setToX(isOn ? 20 : 0);
            fillAnimation.setFromValue(isOn ? Color.WHITE : Color.web("#1e88e5"));
            fillAnimation.setToValue(isOn ? Color.web("#1e88e5") : Color.WHITE);
            background.setStroke(isOn ? Color.web("#1e88e5") : Color.LIGHTGRAY);
            // trigger.setStroke(isOn ? Color.web("#1e88e5") : Color.LIGHTGRAY);

            animation.play();
        });

        setOnMouseClicked(event -> {
            switchedOn.set(!switchedOn.get());
        });

        // Initial state logic to set colors without animation
        // This is a bit tricky with bindings, so we'll just let the listener handle it
        // or init it
    }

    // Method to set state without triggering animation (e.g. for table loading)
    public void setState(boolean on) {
        switchedOn.set(on);
        Rectangle background = (Rectangle) getChildren().get(0);
        Circle trigger = (Circle) getChildren().get(1);

        trigger.setTranslateX(on ? 20 : 0);
        background.setFill(on ? Color.web("#1e88e5") : Color.WHITE);
        background.setStroke(on ? Color.web("#1e88e5") : Color.LIGHTGRAY);
    }
}
