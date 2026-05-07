package com.mycompany.ventacontrolfx.shared.util;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.BiConsumer;

public class ServerPaginationHelper<T> {
    private final TableView<T> table;
    private final ComboBox<Integer> comboLimit;
    private final Label lblCount;
    private final Pagination pagination;
    private final String entityNamePlural;
    private java.util.ResourceBundle bundle;

    // Callback: offset, limit
    private final BiConsumer<Integer, Integer> fetchDataCallback;
    private int totalItemCount;
    private int currentOffset;
    private int currentLimit;

    public ServerPaginationHelper(TableView<T> table, ComboBox<Integer> comboLimit, Label lblCount,
            Pagination pagination, String entityNamePlural, BiConsumer<Integer, Integer> fetchDataCallback) {
        this(table, comboLimit, lblCount, pagination, entityNamePlural, fetchDataCallback, null);
    }

    public ServerPaginationHelper(TableView<T> table, ComboBox<Integer> comboLimit, Label lblCount,
            Pagination pagination, String entityNamePlural, BiConsumer<Integer, Integer> fetchDataCallback,
            java.util.ResourceBundle bundle) {
        this.table = table;
        this.comboLimit = comboLimit;
        this.lblCount = lblCount;
        this.pagination = pagination;
        this.entityNamePlural = entityNamePlural;
        this.fetchDataCallback = fetchDataCallback;
        this.bundle = bundle;
        setup();
    }

    private void setup() {
        if (comboLimit != null) {
            comboLimit.setItems(FXCollections.observableArrayList(10, 25, 50, 100, 500));
            comboLimit.setValue(500);
            comboLimit.setOnAction(e -> {
                if (pagination != null)
                    pagination.setCurrentPageIndex(0);
                triggerLoad(0);
            });
        }
        if (pagination != null) {
            pagination.setPageFactory(this::createPage);
        }
    }

    public void jumpPages(int delta) {
        if (pagination == null)
            return;
        int currentPage = pagination.getCurrentPageIndex();
        int pageCount = pagination.getPageCount();
        int nextPage = currentPage + delta;

        if (nextPage < 0)
            nextPage = 0;
        if (nextPage >= pageCount)
            nextPage = pageCount - 1;

        if (nextPage != currentPage) {
            pagination.setCurrentPageIndex(nextPage);
            // pagination.setCurrentPageIndex triggers the PageFactory (createPage) which
            // triggers triggerLoad
        }
    }

    // Called by the Controller after receiving async results
    public void applyDataTarget(List<T> data, int totalCount) {
        this.totalItemCount = totalCount;
        int limit = getLimit();
        int pages = (int) Math.ceil((double) totalItemCount / limit);
        if (pages <= 0)
            pages = 1;

        if (pagination != null) {
            if (pagination.getPageCount() != pages) {
                pagination.setPageCount(pages);
            }
        }

        table.setItems(FXCollections.observableArrayList(data));

        int end = Math.min(currentOffset + limit, totalItemCount);
        int start = totalItemCount == 0 ? 0 : currentOffset + 1;
        updateCountLabel(start, end, totalItemCount);
    }

    public void refresh() {
        int currentIndex = pagination != null ? pagination.getCurrentPageIndex() : 0;
        triggerLoad(currentIndex);
    }

    private javafx.scene.Node createPage(int pageIndex) {
        triggerLoad(pageIndex);
        return new javafx.scene.layout.VBox(); // dummy empty node, table sits externally
    }

    private void triggerLoad(int pageIndex) {
        currentLimit = getLimit();
        currentOffset = pageIndex * currentLimit;
        if (fetchDataCallback != null) {
            fetchDataCallback.accept(currentOffset, currentLimit);
        }
    }

    private int getLimit() {
        return (comboLimit != null && comboLimit.getValue() != null) ? comboLimit.getValue() : 50;
    }

    private void updateCountLabel(int start, int end, int total) {
        if (lblCount != null) {
            String format = (bundle != null && bundle.containsKey("pagination.showing_format"))
                    ? bundle.getString("pagination.showing_format")
                    : "Mostrando %d - %d de %d %s";
            lblCount.setText(String.format(format, start, end, total, entityNamePlural));
        }
    }
}

