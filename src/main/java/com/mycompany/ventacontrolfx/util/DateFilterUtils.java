package com.mycompany.ventacontrolfx.util;

import javafx.scene.control.DatePicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to add standardized quick date filters to any history view.
 */
public class DateFilterUtils {

    /**
     * Standard version that updates two DatePickers.
     */
    public static void addQuickFilters(HBox container, DatePicker start, DatePicker end, java.util.ResourceBundle bundle, Runnable onFilter) {
        addQuickFilters(container, (label) -> {
            if (start == null || end == null)
                return;
            if (bundle != null) {
                if (label.equals(bundle.getString("filter.date.today"))) {
                    start.setValue(LocalDate.now());
                    end.setValue(LocalDate.now());
                } else if (label.equals(bundle.getString("filter.date.7d"))) {
                    start.setValue(LocalDate.now().minusDays(7));
                    end.setValue(LocalDate.now());
                } else if (label.equals(bundle.getString("filter.date.this_month"))) {
                    start.setValue(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
                    end.setValue(LocalDate.now());
                } else {
                    start.setValue(null);
                    end.setValue(null);
                }
            } else {
                switch (label) {
                    case "Hoy":
                        start.setValue(LocalDate.now());
                        end.setValue(LocalDate.now());
                        break;
                    case "7D":
                        start.setValue(LocalDate.now().minusDays(7));
                        end.setValue(LocalDate.now());
                        break;
                    case "Este Mes":
                        start.setValue(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
                        end.setValue(LocalDate.now());
                        break;
                    case "Todo":
                    default:
                        start.setValue(null);
                        end.setValue(null);
                        break;
                }
            }
        }, bundle, onFilter);
    }

    /**
     * Standard version that updates two DatePickers. (Legacy support)
     */
    public static void addQuickFilters(HBox container, DatePicker start, DatePicker end, Runnable onFilter) {
        addQuickFilters(container, start, end, null, onFilter);
    }

    /**
     * Flexible version that just provides the label to a range setter consumer.
     */
    public static void addQuickFilters(HBox container, java.util.function.Consumer<String> rangeSetter,
            java.util.ResourceBundle bundle, Runnable onFilter) {
        if (container == null)
            return;

        container.getChildren().clear();
        if (!container.getStyleClass().contains("date-filter-container")) {
            container.getStyleClass().add("date-filter-container");
        }

        ToggleGroup group = new ToggleGroup();
        List<ToggleButton> buttons = new ArrayList<>();

        if (bundle != null) {
            buttons.add(createFilterChip(bundle.getString("filter.date.today"), rangeSetter, group, onFilter));
            buttons.add(createFilterChip(bundle.getString("filter.date.7d"), rangeSetter, group, onFilter));
            buttons.add(createFilterChip(bundle.getString("filter.date.this_month"), rangeSetter, group, onFilter));

            ToggleButton btnTodo = createFilterChip(bundle.getString("filter.date.all"), rangeSetter, group, onFilter);
            buttons.add(btnTodo);
            btnTodo.setSelected(true);
            rangeSetter.accept(bundle.getString("filter.date.all"));
        } else {
            buttons.add(createFilterChip("Hoy", rangeSetter, group, onFilter));
            buttons.add(createFilterChip("7D", rangeSetter, group, onFilter));
            buttons.add(createFilterChip("Este Mes", rangeSetter, group, onFilter));

            ToggleButton btnTodo = createFilterChip("Todo", rangeSetter, group, onFilter);
            buttons.add(btnTodo);
            btnTodo.setSelected(true);
            rangeSetter.accept("Todo");
        }

        container.getChildren().addAll(buttons);
    }

    /**
     * Flexible version that just provides the label to a range setter consumer. (Legacy support)
     */
    public static void addQuickFilters(HBox container, java.util.function.Consumer<String> rangeSetter,
            Runnable onFilter) {
        addQuickFilters(container, rangeSetter, null, onFilter);
    }

    private static ToggleButton createFilterChip(String text, java.util.function.Consumer<String> rangeSetter,
            ToggleGroup group, Runnable onFilter) {
        ToggleButton btn = new ToggleButton(text);
        btn.getStyleClass().add("date-filter-chip");
        btn.setToggleGroup(group);

        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                rangeSetter.accept(text);
                if (onFilter != null) {
                    onFilter.run();
                }
            } else {
                // Keep selected if clicked again
                btn.setSelected(true);
            }
        });

        return btn;
    }
}
