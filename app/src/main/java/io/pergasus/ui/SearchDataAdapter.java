/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.pergasus.R;
import io.pergasus.api.DataLoadingSubject;
import io.pergasus.api.ProductItem;
import io.pergasus.api.ProductItemSorting.NaturalOrderWeigher;
import io.pergasus.api.ProductItemSorting.ProductItemComparator;
import io.pergasus.api.ProductItemSorting.ProductItemGroupWeigher;
import io.pergasus.api.ProductWeigher;
import io.pergasus.data.Product;
import io.pergasus.ui.widget.BadgedFourThreeImageView;
import io.pergasus.util.ObservableColorMatrix;
import io.pergasus.util.TransitionUtils;
import io.pergasus.util.ViewUtils;
import io.pergasus.util.glide.GlideApp;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static io.pergasus.util.AnimUtils.getFastOutSlowInInterpolator;

/**
 * Project : phoenix-master
 * Created by Dennis Bilson on Wed at 12:45 PM.
 * Package name : io.pergasus.ui
 */

public class SearchDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements DataLoadingSubject.DataLoadingCallbacks,
		ListPreloader.PreloadModelProvider<Product> {
	static final int REQUEST_CODE_VIEW_PRODUCT = 5407;
	private static final String TAG = "SearchDataAdapter";
	
	private static final int TYPE_PRODUCT = 0;
	private static final int TYPE_LOADING_MORE = -1;
	
	// we need to hold on to an activity ref for the shared element transitions :/
	final Activity host;
	private final LayoutInflater layoutInflater;
	private final ProductItemComparator comparator;
	@Nullable
	private final DataLoadingSubject dataLoading;
	private final int columns;
	private final ColorDrawable[] shotLoadingPlaceholders;
	private final ViewPreloadSizeProvider<Product> shotPreloadSizeProvider;
	
	@ColorInt
	private final
	int initialGifBadgeColor;
	private final List<ProductItem> items;
	private boolean showLoadingMore;
	private NaturalOrderWeigher naturalOrderWeigher;
	private ProductWeigher shotWeigher;
	
	//Constructor
	public SearchDataAdapter(Activity hostActivity,
	                         @Nullable DataLoadingSubject dataLoading,
	                         int columns,
	                         ViewPreloadSizeProvider<Product> shotPreloadSizeProvider) {
		this.host = hostActivity;
		this.dataLoading = dataLoading;
		if (dataLoading != null) {
			dataLoading.registerCallback(this);
		}
		this.columns = columns;
		this.shotPreloadSizeProvider = shotPreloadSizeProvider;
		layoutInflater = LayoutInflater.from(host);
		comparator = new ProductItemComparator();
		items = new ArrayList<>(0);
		setHasStableIds(true);
		
		// get the dribbble shot placeholder colors & badge color from the theme
		TypedArray a = host.obtainStyledAttributes(R.styleable.DribbbleFeed);
		int loadingColorArrayId =
				a.getResourceId(R.styleable.DribbbleFeed_shotLoadingPlaceholderColors, 0);
		if (loadingColorArrayId == 0) {
			shotLoadingPlaceholders = new ColorDrawable[]{new ColorDrawable(Color.DKGRAY)};
		} else {
			int[] placeholderColors = host.getResources().getIntArray(loadingColorArrayId);
			shotLoadingPlaceholders = new ColorDrawable[placeholderColors.length];
			for (int i = 0; i < placeholderColors.length; i++) {
				shotLoadingPlaceholders[i] = new ColorDrawable(placeholderColors[i]);
			}
		}
		int initialGifBadgeColorId =
				a.getResourceId(R.styleable.DribbbleFeed_initialBadgeColor, 0);
		initialGifBadgeColor = initialGifBadgeColorId == 0 ? 0x40ffffff : ContextCompat.getColor(host, initialGifBadgeColorId);
		a.recycle();
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_PRODUCT:
				return createProductHolder(parent);
			case TYPE_LOADING_MORE:
				return new LoadingMoreHolder(
						layoutInflater.inflate(R.layout.infinite_loading, parent, false));
		}
		return null;
	}
	
	private ProductViewHolder createProductHolder(ViewGroup parent) {
		ProductViewHolder holder = new ProductViewHolder(layoutInflater.inflate(R.layout.search_item,
				parent, false));
		holder.image.setBadgeColor(initialGifBadgeColor);
		holder.image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				//show details view
				Intent intent = new Intent();
				intent.setClass(host, DetailsActivity.class);
				intent.putExtra(DetailsActivity.Companion.getEXTRA_SHOT(),
						(Product) SearchDataAdapter.this.getItem(holder.getAdapterPosition()));
				SearchDataAdapter.this.setGridItemContentTransitions(holder.image);
				ActivityOptions options =
						ActivityOptions.makeSceneTransitionAnimation(host,
								Pair.create(view, host.getString(R.string.transition_shot)),
								Pair.create(view, host.getString(R.string
										.transition_shot_background)));
				host.startActivityForResult(intent, REQUEST_CODE_VIEW_PRODUCT, options.toBundle());
			}
		});
		// play animated GIFs whilst touched
		holder.image.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// check if it's an event we care about, else bail fast
				int action = event.getAction();
				if (!(action == MotionEvent.ACTION_DOWN
						|| action == MotionEvent.ACTION_UP
						|| action == MotionEvent.ACTION_CANCEL)) return false;
				
				// get the image and check if it's an animated GIF
				Drawable drawable = holder.image.getDrawable();
				if (drawable == null) return false;
				GifDrawable gif = null;
				if (drawable instanceof GifDrawable) {
					gif = (GifDrawable) drawable;
				} else if (drawable instanceof TransitionDrawable) {
					// we fade in images on load which uses a TransitionDrawable; check its layers
					TransitionDrawable fadingIn = (TransitionDrawable) drawable;
					for (int i = 0; i < fadingIn.getNumberOfLayers(); i++) {
						if (fadingIn.getDrawable(i) instanceof GifDrawable) {
							gif = (GifDrawable) fadingIn.getDrawable(i);
							break;
						}
					}
				}
				if (gif == null) return false;
				// GIF found, start/stop it on press/lift
				switch (action) {
					case MotionEvent.ACTION_DOWN:
						gif.start();
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						gif.stop();
						break;
				}
				return false;
			}
		});
		return holder;
	}
	
	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		switch (getItemViewType(position)) {
			case TYPE_PRODUCT:
				bindProductHolder((Product) getItem(position), (ProductViewHolder) holder, position);
				break;
			case TYPE_LOADING_MORE:
				bindLoadingViewHolder((LoadingMoreHolder) holder, position);
				break;
		}
	}
	
	private void bindLoadingViewHolder(LoadingMoreHolder holder, int position) {
		// only show the infinite load progress spinner if there are already items in the
		// grid i.e. it's not the first item & data is being loaded
		holder.progress.setVisibility((position > 0
				&& dataLoading != null
				&& dataLoading.isDataLoading())
				? View.VISIBLE : View.INVISIBLE);
	}
	
	@SuppressLint("Range")
	private void bindProductHolder(Product shot, ProductViewHolder holder, int position) {
		GlideApp.with(host)
				.load(shot.getUrl())
				.listener(new RequestListener<Drawable>() {
					@Override
					public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
						return false;
					}
					
					@Override
					public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
						if (!shot.getHasFadedIn()) {
							holder.image.setHasTransientState(true);
							ObservableColorMatrix cm = new ObservableColorMatrix();
							ObjectAnimator saturation = ObjectAnimator.ofFloat(
									cm, ObservableColorMatrix.SATURATION, 0.0f, 1.0f);
							saturation.addUpdateListener(new AnimatorUpdateListener() {
								@Override
								public void onAnimationUpdate(ValueAnimator valueAnimator) {
									// just animating the color matrix does not invalidate the
									// drawable so need this update listener.  Also have to create a
									// new CMCF as the matrix is immutable :(
									holder.image.setColorFilter(new ColorMatrixColorFilter(cm));
								}
							});
							saturation.setDuration(2000L);
							saturation.setInterpolator(getFastOutSlowInInterpolator(host));
							saturation.addListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									holder.image.clearColorFilter();
									holder.image.setHasTransientState(false);
								}
							});
							saturation.start();
							shot.setHasFadedIn(true);
						}
						return false;
					}
				})
				.apply(RequestOptions.placeholderOf(shotLoadingPlaceholders[position % shotLoadingPlaceholders.length]))
				.apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.DATA))
				.apply(RequestOptions.fitCenterTransform())
				.apply(RequestOptions.overrideOf(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
				.transition(withCrossFade())
				.into(holder.image);
		// need both placeholder & background to prevent seeing through shot as it fades in
		holder.image.setBackground(
				shotLoadingPlaceholders[position % shotLoadingPlaceholders.length]);
		holder.image.setDrawBadge(shot.getAnimated());
		// need a unique transition name per shot, let's use it's url
		holder.image.setTransitionName(shot.getUrl());
		shotPreloadSizeProvider.setView(holder.image);
	}
	
	@Override
	public int getItemViewType(int position) {
		if (position < getDataItemCount()
				&& getDataItemCount() > 0) {
			ProductItem item = getItem(position);
			if (item instanceof Product) {
				return TYPE_PRODUCT;
			}
		}
		return TYPE_LOADING_MORE;
	}
	
	@Override
	public void onViewRecycled(ViewHolder holder) {
		if (holder instanceof ProductViewHolder) {
			ProductViewHolder viewHolder = (ProductViewHolder) holder;
			viewHolder.image.setBadgeColor(initialGifBadgeColor);
			viewHolder.image.setDrawBadge(false);
			viewHolder.image.setForeground(ContextCompat.getDrawable(host, R.drawable.mid_grey_ripple));
		}
	}
	
	@Override
	public long getItemId(int position) {
		if (getItemViewType(position) == TYPE_LOADING_MORE) {
			return -1L;
		}
		return ((long) getItem(position).hashCode());
	}
	
	int getItemPosition(long itemId) {
		for (int position = 0; position < items.size(); position++) {
			if (getItem(position).getId() == itemId) return position;
		}
		return RecyclerView.NO_POSITION;
	}
	
	@Override
	public int getItemCount() {
		return getDataItemCount() + (showLoadingMore ? 1 : 0);
	}
	
	@NonNull
	@Override
	public List<Product> getPreloadItems(int position) {
		ProductItem item = getItem(position);
		if (item instanceof Product) {
			return Collections.singletonList((Product) item);
		}
		return Collections.emptyList();
	}
	
	@Nullable
	@Override
	public RequestBuilder<?> getPreloadRequestBuilder(Product item) {
		return GlideApp.with(host).load(item.getUrl());
	}
	
	@Override
	public void dataStartedLoading() {
		if (showLoadingMore) return;
		showLoadingMore = true;
		notifyItemInserted(getLoadingMoreItemPosition());
	}
	
	@Override
	public void dataFinishedLoading() {
		if (!showLoadingMore) return;
		int loadingPos = getLoadingMoreItemPosition();
		showLoadingMore = false;
		notifyItemRemoved(loadingPos);
	}
	
	/*Custom*/
	@Nullable
	private ProductItem getItem(int position) {
		if (position < 0 || position >= items.size()) return null;
		return items.get(position);
	}
	
	int getItemColumnSpan(int position) {
		switch (getItemViewType(position)) {
			case TYPE_LOADING_MORE:
				return columns;
			default:
				return getItem(position).getColspan();
		}
	}
	
	public void clear() {
		items.clear();
		notifyDataSetChanged();
	}
	
	/**
	 * Main entry point for adding items to this adapter. Takes care of de-duplicating items and
	 * sorting them (depending on the data source). Will also expand some items to span multiple
	 * grid columns.
	 */
	void addAndResort(List<? extends ProductItem> newItems) {
		Log.d(TAG, "addAndResort: " + newItems.toString());
		weighItems(newItems);
		deduplicateAndAdd(newItems);
		sort();
		expandPopularItems();
		notifyDataSetChanged();
	}
	
	/**
	 * Calculate a 'weight' [0, 1] for each data type for sorting. Each data type/source has a
	 * different metric for weighing it e.g. likes etc. but some sources should keep
	 * the order returned by the API. Weights are 'scoped' to the page they belong to and lower
	 * weights are sorted earlier in the grid (i.e. in ascending weight).
	 */
	private void weighItems(List<? extends ProductItem> items) {
		if (items == null || items.isEmpty()) return;
		ProductItemGroupWeigher weigher = null;
		if (items.get(0) instanceof Product) {
			if (shotWeigher == null) shotWeigher = new ProductWeigher();
			weigher = shotWeigher;
		}
		if (weigher != null) {
			weigher.weigh(items);
		}
	}
	
	/**
	 * De-dupe as the same item can be returned by multiple feeds
	 */
	private void deduplicateAndAdd(List<? extends ProductItem> newItems) {
		int count = getDataItemCount();
		for (ProductItem newItem : newItems) {
			boolean add = true;
			for (int i = 0; i < count; i++) {
				ProductItem existingItem = getItem(i);
				if (existingItem != null && existingItem.equals(newItem)) {
					add = false;
					break;
				}
			}
			if (add) {
				add(newItem);
			}
		}
	}
	
	private void add(ProductItem item) {
		items.add(item);
	}
	
	private void sort() {
		Collections.sort(items, comparator); // sort by weight
	}
	
	private void expandPopularItems() {
		// for now just expand the first image per page which should be
		// the most popular according to our weighing & sorting
		List<Integer> expandedPositions = new ArrayList<>(0);
		int page = -1;
		int count = items.size();
		for (int i = 0; i < count; i++) {
			ProductItem item = getItem(i);
			if (item instanceof Product && item.getPage() > page) {
				item.setColspan(columns);
				page = item.getPage();
				expandedPositions.add(i);
			} else {
				if (item != null) {
					item.setColspan(1);
				}
			}
		}
		
		// make sure that any expanded items are at the start of a row
		// so that we don't leave any gaps in the grid
		for (int expandedPos = 0; expandedPos < expandedPositions.size(); expandedPos++) {
			int pos = expandedPositions.get(expandedPos);
			int extraSpannedSpaces = expandedPos * (columns - 1);
			int rowPosition = (pos + extraSpannedSpaces) % columns;
			if (rowPosition != 0) {
				int swapWith = pos + (columns - rowPosition);
				if (swapWith < items.size()) {
					Collections.swap(items, pos, swapWith);
				}
			}
		}
	}
	
	void removeDataSource(String dataSource) {
		for (int i = items.size() - 1; i >= 0; i--) {
			ProductItem item = items.get(i);
			if (dataSource.equals(item.getDataSource())) {
				items.remove(i);
			}
		}
		sort();
		expandPopularItems();
		notifyDataSetChanged();
	}
	
	/**
	 * The shared element transition to dribbble shots & dn stories can intersect with the FAB.
	 * This can cause a strange layers-passing-through-each-other effect. On return hide the FAB
	 * and animate it back in after the transition.
	 */
	private void setGridItemContentTransitions(View gridItem) {
		View fab = host.findViewById(R.id.fab);
		if (!ViewUtils.viewsIntersect(gridItem, fab)) return;
		
		Transition reenter = TransitionInflater.from(host)
				.inflateTransition(R.transition.grid_overlap_fab_reenter);
		reenter.addListener(new TransitionUtils.TransitionListenerAdapter() {
			
			@Override
			public void onTransitionEnd(Transition transition) {
				// we only want these content transitions in certain cases so clear out when done.
				host.getWindow().setReenterTransition(null);
			}
		});
		host.getWindow().setReenterTransition(reenter);
	}
	
	int getDataItemCount() {
		return items.size();
	}
	
	private int getLoadingMoreItemPosition() {
		return showLoadingMore ? getItemCount() - 1 : RecyclerView.NO_POSITION;
	}
	
	@NonNull
	static SharedElementCallback createSharedElementReenterCallback(
			@NonNull Context context) {
		String shotTransitionName = context.getString(R.string.transition_shot);
		String shotBackgroundTransitionName =
				context.getString(R.string.transition_shot_background);
		return new SharedElementCallback() {
			
			/**
			 * We're performing a slightly unusual shared element transition i.e. from one view
			 * (image in the grid) to two views (the image & also the background of the details
			 * view, to produce the expand effect). After changing orientation, the transition
			 * system seems unable to map both shared elements (only seems to map the shot, not
			 * the background) so in this situation we manually map the background to the
			 * same view.
			 */
			@Override
			public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
				if (sharedElements.size() != names.size()) {
					// couldn't map all shared elements
					View sharedShot = sharedElements.get(shotTransitionName);
					if (sharedShot != null) {
						// has shot so add shot background, mapped to same view
						sharedElements.put(shotBackgroundTransitionName, sharedShot);
					}
				}
			}
		};
	}
	
	/*package*/static class ProductViewHolder extends RecyclerView.ViewHolder {
		BadgedFourThreeImageView image;
		
		ProductViewHolder(View itemView) {
			super(itemView);
			image = itemView.findViewById(R.id.shot);
		}
	}
	
	/*package*/static class LoadingMoreHolder extends RecyclerView.ViewHolder {
		ProgressBar progress;
		
		LoadingMoreHolder(View itemView) {
			super(itemView);
			progress = (ProgressBar) itemView;
		}
		
	}
}
