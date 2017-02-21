package com.ztiany.adapter;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.ztiany.loadmore.ItemFullSpanProvider;
import com.ztiany.loadmore.LoadMode;
import com.ztiany.loadmore.LoadMoreViewFactory;
import com.ztiany.loadmore.OnLoadMoreListener;
import com.ztiany.loadmore.OnRecyclerViewScrollBottomListener;

import java.util.List;

import static android.support.v7.widget.RecyclerView.Adapter;
import static android.support.v7.widget.RecyclerView.ViewHolder;


public class WrapperAdapter extends RecyclerViewAdapterWrapper implements LoadMoreManager, StateManager {

    private static final String TAG = WrapperAdapter.class.getSimpleName();
    private static final int LOAD_MORE_FOOTER = Integer.MAX_VALUE;//loading Footer
    private LoadMoreImpl mLoadMore;
    private StateImpl mState;
    private OnRecyclerViewScrollBottomListener mScrollListener;
    private boolean mEnableLoadMore = true;

    RecyclerView mRecyclerView;
    GridLayoutManager.SpanSizeLookup mSpanSizeLookup;
    ItemFullSpanProvider mItemFullSpanProvider;


    public static WrapperAdapter setup(RecyclerView.Adapter adapter, RecyclerView recyclerView) {
        WrapperAdapter wrapperAdapter = new WrapperAdapter(adapter);
        wrapperAdapter.mRecyclerView = recyclerView;
        wrapperAdapter.initOnAttachedToRecyclerView();
        recyclerView.setAdapter(wrapperAdapter);
        return wrapperAdapter;
    }

    private WrapperAdapter(Adapter wrapped) {
        super(wrapped);

        mLoadMore = new LoadMoreImpl();
        mState = new StateImpl(this, getWrappedAdapter());
        mScrollListener = new OnRecyclerViewScrollBottomListener() {
            @Override
            public void onBottom() {
                if (mEnableLoadMore) {
                    mLoadMore.tryCallLoadMore();
                }
            }
        };
    }

    @SuppressWarnings("unused")
    public void setLoadingTriggerThreshold(int loadingTriggerThreshold) {
        mScrollListener.setLoadingTriggerThreshold(loadingTriggerThreshold);
    }

    @SuppressWarnings("unused")
    public void setAdapterInterface(AdapterInterface lastVisibleItemPosition) {
        mItemFullSpanProvider = lastVisibleItemPosition;
        mScrollListener.setLastVisibleItemPositionGetter(lastVisibleItemPosition);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mState.needProcess()) {
            return mState.onCreateViewHolder(parent, viewType);
        }
        if (viewType == LOAD_MORE_FOOTER) {
            View loadMoreView = mLoadMore.getLoadMoreView(parent);
            return new ViewHolder(loadMoreView) {
            };
        } else {
            return super.onCreateViewHolder(parent, viewType);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mState.needProcess()) {
            mState.onBindViewHolder(holder, position);
            return;
        }
        if (getItemViewType(position) == LOAD_MORE_FOOTER) {

            KeepFullSpanUtils.keepFullSpan(
                    holder.itemView,
                    mRecyclerView,
                    false,
                    mSpanSizeLookup,
                    mItemFullSpanProvider);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List payloads) {
        if (mState.needProcess()) {
            mState.onBindViewHolder(holder, position);
            return;
        }
        if (getItemViewType(position) == LOAD_MORE_FOOTER) {

            KeepFullSpanUtils.keepFullSpan(
                    holder.itemView,
                    mRecyclerView,
                    false,
                    mSpanSizeLookup,
                    mItemFullSpanProvider);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }


    @Override
    public int getItemCount() {
        if (mState.needProcess()) {
            return mState.getCount();
        }
        int count = super.getItemCount();
        if (mEnableLoadMore) {
            return count == 0 ? count : count + 1;
        } else {
            return count;
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (mState.needProcess()) {
            return mState.getType(position);
        }
        if (mEnableLoadMore && position == super.getItemCount()) {
            return LOAD_MORE_FOOTER;
        } else {
            return super.getItemViewType(position);
        }
    }

    private boolean isWrapperType(int position) {
        return getItemViewType(position) == LOAD_MORE_FOOTER;
    }

    @Override
    public long getItemId(int position) {
        if (!isWrapperType(position)) {
            return super.getItemId(position);
        }
        return position;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewRecycled(ViewHolder holder) {
        if (mState.needProcess() || isLoadMoreOrStateViewHolder(holder)) {
            return;
        }
        super.onViewRecycled(holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onFailedToRecycleView(ViewHolder holder) {
        return !(mState.needProcess() || isLoadMoreOrStateViewHolder(holder)) && super.onFailedToRecycleView(holder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        if (mState.needProcess() || isLoadMoreOrStateViewHolder(holder)) {
            return;
        }
        super.onViewAttachedToWindow(holder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        if (mState.needProcess() || isLoadMoreOrStateViewHolder(holder)) {
            return;
        }
        super.onViewDetachedFromWindow(holder);
    }

    private void initOnAttachedToRecyclerView() {

        mRecyclerView.removeOnScrollListener(mScrollListener);
        mRecyclerView.addOnScrollListener(mScrollListener);
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            mSpanSizeLookup = layoutManager.getSpanSizeLookup();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.removeOnScrollListener(mScrollListener);
    }

    private boolean isLoadMoreOrStateViewHolder(ViewHolder viewHolder) {
        int itemViewType = viewHolder.getItemViewType();
        return itemViewType == LOAD_MORE_FOOTER
                || mState.isNotContentStateViewType(itemViewType);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Status
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void content() {
        mState.content();
    }

    @Override
    public void fail() {
        mState.fail();
    }

    @Override
    public void empty() {
        mState.empty();
    }

    @Override
    public void loading() {
        mState.loading();
    }

    @Override
    public void setStateViewFactory(StateViewFactory stateViewFactory) {
        mState.setStateViewFactory(stateViewFactory);
    }

    ///////////////////////////////////////////////////////////////////////////
    // LoadMoreManager
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        mLoadMore.setOnLoadMoreListener(onLoadMoreListener);
    }

    @Override
    public void loadFail() {
        mLoadMore.loadFail();
    }

    @Override
    public void loadCompleted(boolean hasMore) {
        mLoadMore.loadCompleted(hasMore);
    }

    @Override
    public boolean isLoadingMore() {
        return mLoadMore.isLoadingMore();
    }

    @Override
    public void setLoadMode(@LoadMode int loadMore) {
        mLoadMore.setLoadMode(loadMore);
    }

    @Override
    public void setLoadMoreViewFactory(LoadMoreViewFactory factory) {
        mLoadMore.setLoadMoreViewFactory(factory);
    }

}