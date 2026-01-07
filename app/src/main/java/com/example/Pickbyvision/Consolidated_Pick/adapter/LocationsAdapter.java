package com.example.Pickbyvision.Consolidated_Pick.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.Pickbyvision.Consolidated_Pick.location.Location;
import com.example.Pickbyvision.R;

import java.util.Collections;
import java.util.List;

public class LocationsAdapter {

    public interface OnRowSelectedListener {
        void onRowSelected(int absoluteIndex);
    }

    private final Context context;
    private final ScrollView tableScrollView;
    private final TableLayout tableLayout;
    private final TextView tvPage;
    private final TextView tvNoLocations;
    private final View arrowUp;
    private final View arrowDown;
    private final int rowsPerPage;
    private final OnRowSelectedListener rowSelectedListener;

    private List<Location> allLocations = Collections.emptyList();
    private int selectedRowIndex = -1;
    private int currentPage = 1;
    private int currentPageStartIndex = 0;

    public LocationsAdapter(
            Context context,
            ScrollView tableScrollView,
            TableLayout tableLayout,
            TextView tvPage,
            TextView tvNoLocations,
            View arrowUp,
            View arrowDown,
            int rowsPerPage,
            OnRowSelectedListener rowSelectedListener
    ) {
        this.context = context;
        this.tableScrollView = tableScrollView;
        this.tableLayout = tableLayout;
        this.tvPage = tvPage;
        this.tvNoLocations = tvNoLocations;
        this.arrowUp = arrowUp;
        this.arrowDown = arrowDown;
        this.rowsPerPage = rowsPerPage;
        this.rowSelectedListener = rowSelectedListener;
    }

    public void submitLocations(List<Location> locations, int explicitIndex, String preselectLocationCode) {
        this.allLocations = (locations == null) ? Collections.emptyList() : locations;

        if (allLocations.isEmpty()) {
            showEmptyState();
            return;
        }

        if (selectedRowIndex >= allLocations.size()) {
            selectedRowIndex = Math.max(0, allLocations.size() - 1);
        }

        if (explicitIndex >= 0 && explicitIndex < allLocations.size()) {
            selectedRowIndex = explicitIndex;
            currentPage = (selectedRowIndex / rowsPerPage) + 1;
        } else if (!n(preselectLocationCode).isEmpty()) {
            int idx = -1;
            for (int i = 0; i < allLocations.size(); i++) {
                Location loc = allLocations.get(i);
                if (loc != null && preselectLocationCode.equalsIgnoreCase(n(loc.getLocationCode()))) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                selectedRowIndex = idx;
                currentPage = (selectedRowIndex / rowsPerPage) + 1;
            } else {
                currentPage = 1;
                selectedRowIndex = 0;
            }
        } else {
            if (selectedRowIndex < 0 || selectedRowIndex >= allLocations.size()) {
                selectedRowIndex = 0;
            }
            currentPage = (selectedRowIndex / rowsPerPage) + 1;
        }

        tableLayout.setVisibility(View.VISIBLE);
        tvNoLocations.setVisibility(View.GONE);

        if (allLocations.size() > 1) {
            arrowUp.setVisibility(View.VISIBLE);
            arrowDown.setVisibility(View.VISIBLE);
        } else {
            arrowUp.setVisibility(View.GONE);
            arrowDown.setVisibility(View.GONE);
        }

        displayCurrentPage();
        updateArrowStates();
    }

    public boolean hasData() {
        return allLocations != null && !allLocations.isEmpty();
    }

    public int getItemCount() {
        return allLocations == null ? 0 : allLocations.size();
    }

    public int getSelectedIndex() {
        return selectedRowIndex;
    }

    public Location getSelectedLocation() {
        if (!hasData()) return null;
        if (selectedRowIndex < 0 || selectedRowIndex >= allLocations.size()) return null;
        return allLocations.get(selectedRowIndex);
    }

    public int calculateTotalQuantitySum() {
        int totalSum = 0;
        if (allLocations != null && !allLocations.isEmpty()) {
            for (Location location : allLocations) {
                if (location != null) totalSum += location.getQuantity();
            }
        }
        return totalSum;
    }

    public boolean canNavigateDown() {
        return hasData() && selectedRowIndex < allLocations.size() - 1;
    }

    public boolean canNavigateUp() {
        return hasData() && selectedRowIndex > 0;
    }

    public void navigateUp() {
        if (!canNavigateUp()) return;

        selectedRowIndex--;
        int newPage = (selectedRowIndex / rowsPerPage) + 1;

        if (newPage != currentPage) {
            currentPage = newPage;
            displayCurrentPage();
        } else {
            refreshRowHighlights();
            scrollToSelectedRow();
        }
        updateArrowStates();
    }

    public void navigateDown() {
        if (!canNavigateDown()) return;

        selectedRowIndex++;
        int newPage = (selectedRowIndex / rowsPerPage) + 1;

        if (newPage != currentPage) {
            currentPage = newPage;
            displayCurrentPage();
        } else {
            refreshRowHighlights();
            scrollToSelectedRow();
        }
        updateArrowStates();
    }

    public boolean isViewInTable(View view) {
        if (view == null) return false;
        if (view == tableLayout) return true;

        View parent = (View) view.getParent();
        while (parent != null) {
            if (parent == tableLayout) return true;
            if (parent.getParent() instanceof View) parent = (View) parent.getParent();
            else parent = null;
        }
        return false;
    }

    private void showEmptyState() {
        tableLayout.setVisibility(View.GONE);
        tvNoLocations.setVisibility(View.VISIBLE);
        tvPage.setText("0/0");
        arrowUp.setVisibility(View.GONE);
        arrowDown.setVisibility(View.GONE);
        selectedRowIndex = -1;
        currentPage = 1;
    }

    private void displayCurrentPage() {
        tableLayout.removeAllViews();

        TableRow headerRow = new TableRow(context);
        headerRow.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        addHeaderCell(headerRow, "Site");
        addHeaderCell(headerRow, "Location");
        addHeaderCell(headerRow, "Qty");
        tableLayout.addView(headerRow);

        currentPageStartIndex = (currentPage - 1) * rowsPerPage;
        int end = Math.min(currentPageStartIndex + rowsPerPage, allLocations.size());

        for (int i = currentPageStartIndex; i < end; i++) {
            Location loc = allLocations.get(i);

            TableRow row = new TableRow(context);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            ));

            final boolean isSelected = (i == selectedRowIndex);

            addDataCell(row, loc == null ? "" : n(loc.getSiteCode()), isSelected);
            addDataCell(row, loc == null ? "" : n(loc.getLocationCode()), isSelected);
            addDataCell(row, loc == null ? "" : String.valueOf(loc.getQuantity()), isSelected);

            final int rowIndex = i;
            row.setOnClickListener(v -> {
                selectedRowIndex = rowIndex;
                refreshRowHighlights();
                updateArrowStates();

                if (rowSelectedListener != null) {
                    rowSelectedListener.onRowSelected(rowIndex);
                }
            });

            tableLayout.addView(row);
        }

        updatePageInfo();
        updateArrowStates();
        tableLayout.post(this::scrollToSelectedRow);
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.bg_table_cell_header);
        tv.setPadding(8, 12, 8, 12);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 1f
        );
        params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(tv);
    }

    private void addDataCell(TableRow row, String text, boolean isSelected) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16f);
        tv.setPadding(8, 12, 8, 12);

        if (isSelected) {
            tv.setBackgroundResource(R.drawable.bg_table_cell_highlight);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        } else {
            tv.setBackgroundResource(R.drawable.bg_table_cell);
            tv.setTextColor(Color.WHITE);
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 1f
        );
        params.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(tv);
    }

    private void refreshRowHighlights() {
        int childCount = tableLayout.getChildCount();

        for (int i = 1; i < childCount; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                int dataIndex = currentPageStartIndex + (i - 1);
                boolean shouldHighlight = (dataIndex == selectedRowIndex);

                for (int j = 0; j < row.getChildCount(); j++) {
                    View cellView = row.getChildAt(j);
                    if (cellView instanceof TextView) {
                        TextView tv = (TextView) cellView;
                        if (shouldHighlight) {
                            tv.setBackgroundResource(R.drawable.bg_table_cell_highlight);
                            tv.setTextColor(Color.BLACK);
                            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                        } else {
                            tv.setBackgroundResource(R.drawable.bg_table_cell);
                            tv.setTextColor(Color.WHITE);
                            tv.setTypeface(null, Typeface.NORMAL);
                        }
                    }
                }
            }
        }

        updatePageInfo();
    }

    private void scrollToSelectedRow() {
        if (tableScrollView == null || tableLayout == null) return;

        int rowIndexInPage = selectedRowIndex - currentPageStartIndex;
        if (rowIndexInPage >= 0 && rowIndexInPage < (tableLayout.getChildCount() - 1)) {
            View selectedRow = tableLayout.getChildAt(rowIndexInPage + 1);
            if (selectedRow != null) {
                int rowTop = selectedRow.getTop();
                int rowBottom = selectedRow.getBottom();
                int scrollY = tableScrollView.getScrollY();
                int height = tableScrollView.getHeight();

                int newScrollY = scrollY;

                if (rowTop < scrollY) {
                    newScrollY = rowTop;
                } else if (rowBottom > scrollY + height) {
                    newScrollY = rowBottom - height;
                }

                if (newScrollY != scrollY) {
                    tableScrollView.smoothScrollTo(0, Math.max(0, newScrollY));
                }
            }
        }
    }

    private void updateArrowStates() {
        boolean hasData = hasData();
        arrowUp.setEnabled(hasData && selectedRowIndex > 0);
        arrowDown.setEnabled(hasData && selectedRowIndex < (getItemCount() - 1));
        arrowUp.setAlpha((hasData && selectedRowIndex > 0) ? 1f : 0.4f);
        arrowDown.setAlpha((hasData && selectedRowIndex < (getItemCount() - 1)) ? 1f : 0.4f);
    }

    private void updatePageInfo() {
        int total = getItemCount();
        int current = (selectedRowIndex >= 0 && selectedRowIndex < total) ? (selectedRowIndex + 1) : 0;
        tvPage.setText(current + "/" + total);
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}