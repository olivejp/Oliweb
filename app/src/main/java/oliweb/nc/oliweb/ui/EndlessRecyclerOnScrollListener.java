package oliweb.nc.oliweb.ui;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class EndlessRecyclerOnScrollListener extends
        RecyclerView.OnScrollListener {
    private int previousTotal = 0;
    private boolean loading = true;
    private LinearLayoutManager mLinearLayoutManager;
    private int currentPage;

    protected EndlessRecyclerOnScrollListener(
            LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = mLinearLayoutManager.getChildCount();
        int totalItemCount = mLinearLayoutManager.getItemCount();
        int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();

        if (loading && totalItemCount > previousTotal) {
            loading = false;
            previousTotal = totalItemCount;
        }

        int visibleThreshold = 5;
        if (!loading
                && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
            currentPage++;
            onLoadMore();
            loading = true;
        }
    }

    public abstract void onLoadMore();
}
