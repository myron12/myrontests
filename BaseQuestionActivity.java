package com.yunxiao.hfs.raise.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.yunxiao.hfs.base.BaseActivity;
import com.yunxiao.hfs.raise.R;
import com.yunxiao.hfs.raise.common.RaiseCommon;
import com.yunxiao.hfs.raise.enums.ExerciseType;
import com.yunxiao.hfs.raise.fragment.BaseQuestionFragment;
import com.yunxiao.hfs.raise.fragment.ExerciseChoiceQuestionFragment;
import com.yunxiao.hfs.raise.fragment.ExerciseMultipleChoiceFragment;
import com.yunxiao.hfs.raise.fragment.ExercisePhotoQuestionFragment;
import com.yunxiao.hfs.raise.listener.OnMultipleChoiceSelectedListener;
import com.yunxiao.hfs.raise.listener.OnQuestionChoiceListener;
import com.yunxiao.log.LogUtils;
import com.yunxiao.ui.YxTitleBar;
import com.yunxiao.yxrequest.raise.entity.PractiseRecord;
import com.yunxiao.yxrequest.raise.entity.latex.Practice;
import com.yunxiao.yxrequest.raise.entity.question.QuestionEntity;

import java.util.ArrayList;

/**
 * 练习题主页面
 */
public abstract class BaseQuestionActivity extends BaseActivity implements View.OnClickListener, OnQuestionChoiceListener, OnGestureFlingListener {
    /**
     * 引导练习的practiceId
     */
    public static final String GUIDE_PROBLEM_PRACTICE_ID = "guide_problem_practice_id";

    public YxTitleBar mTitleView;

    protected ArrayList<QuestionEntity> mList;
    protected PractiseRecord mPractiseRecord;
    private int mCurrentPage = 0;

    private TextView mTvPrev, mTvNext, mTvCur;
    private ExerciseType mCurrentFragType;
    OnMultipleChoiceSelectedListener mMultipleChoiceListener;
    public int currMultipleIndex;

    private BaseQuestionFragment mCurrentFragment;

    public static final String EXTRA_PRACTICEID = "practiceId";
    public static final String EXTRA_POSITION = "position";
    public static final int CODE_NAVIGATION = 1;

    private View mProgressView;
    private View mNoNetworkView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        mTitleView = (YxTitleBar) findViewById(R.id.title);
        mTitleView.setOnLeftButtonClickListener(button -> finish());

        mProgressView = findViewById(R.id.rl_progress_practice);
        mNoNetworkView = findViewById(R.id.rl_no_network_practice);
        showProgress(false);
        showNoNetWork(false);

        mTvPrev = (TextView) findViewById(R.id.last);
        mTvNext = (TextView) findViewById(R.id.next);
        mTvCur = (TextView) findViewById(R.id.page_number);
        mTvPrev.setEnabled(false);
        mTvNext.setEnabled(false);
        mTvCur.setText("0/0");
        mTvPrev.setOnClickListener(this);
        mTvNext.setOnClickListener(this);
    }

    public void setTitle(String title) {
        mTitleView.setTitle(title);
    }

    public void setRightClickListener(YxTitleBar.OnRightButtonClickListener listener) {
        mTitleView.setRightButtonResource(R.drawable.nav_button_nav_selector, listener);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.last) {
            showPreItem();
        } else if (id == R.id.next) {
            showNextItem();
        }
    }

    /**
     * 下一题
     */
    private void showNextItem() {
        Practice practice = mList.get(mCurrentPage).getQuestionObj();
        int size = practice == null ? 1 : practice.blocks.stems.size();
        if (mCurrentFragType == ExerciseType.TYPE_MULTIPLE_CHOICE && currMultipleIndex < size - 1) {
            currMultipleIndex++;
            setBottom(currMultipleIndex, size);
            if (mMultipleChoiceListener != null) {
                mMultipleChoiceListener.onSelected(currMultipleIndex);
            }
        } else if (mCurrentPage < mList.size() - 1) {
            mCurrentPage++;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out);
            switchFragment(transaction, true);
        } else if (mCurrentPage == mList.size() - 1) {
            Intent intent = new Intent(this, PracticeQuestionNavigationActivity.class);
            intent.putExtra(PracticeQuestionNavigationActivity.EXTRA_PRACTICE, mPractiseRecord);
            startActivityForResult(intent, CODE_NAVIGATION);
        }
    }

    /**
     * 上一题
     */
    private void showPreItem() {
        if (mCurrentFragType == ExerciseType.TYPE_MULTIPLE_CHOICE && currMultipleIndex > 0) {
            currMultipleIndex--;
            Practice practice = mList.get(mCurrentPage).getQuestionObj();
            int size = practice == null ? 1 : practice.blocks.stems.size();
            setBottom(currMultipleIndex, size);
            if (mMultipleChoiceListener != null) {
                mMultipleChoiceListener.onSelected(currMultipleIndex);
            }
        } else if (mCurrentPage > 0) {
            mCurrentPage--;

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_left_in, R.anim.slide_right_out);
            switchFragment(transaction, false);
        }
    }

    public void setOptionIndex(int index) {
        currMultipleIndex = index;
        Practice practice = mList.get(mCurrentPage).getQuestionObj();
        int size = practice == null ? 1 : practice.blocks.stems.size();
        setBottom(index, size);
    }

    public void setBottom(int multiplePos, int size) {
        mTvPrev.setEnabled(!(multiplePos == 0 && mCurrentPage == 0));
        mTvPrev.setText(multiplePos == 0 ? "上一题" : "上一问");
        mTvNext.setEnabled(!(multiplePos == size - 1 && mCurrentPage == mList.size() - 1));
        mTvNext.setText(multiplePos == size - 1 ? "下一题" : "下一问");
    }


    public void setOnMultipleChoiceListener(OnMultipleChoiceSelectedListener multipleChoiceListener) {
        mMultipleChoiceListener = multipleChoiceListener;
    }


    public boolean setCurrentPage(int pageIndex) {
        if (mCurrentPage == pageIndex) {
            return false;
        }
        mCurrentPage = pageIndex;
        return true;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }


    public void switchFragment(FragmentTransaction transaction, boolean isNext) {
        QuestionEntity entity = mList.get(mCurrentPage);
        if (entity == null) {
            getData();
            return;
        }
        mCurrentFragType = RaiseCommon.getExerciseType(entity);

        mTvPrev.setEnabled(mCurrentPage != 0);
        mTvNext.setEnabled(mCurrentPage != mList.size() - 1);

        mTvPrev.setText("上一题");
        mTvNext.setText("下一题");
        mTvCur.setText((mCurrentPage + 1) + "/" + mList.size());
        switch (mCurrentFragType) {
            case TYPE_CHOICE_MULTIPLE:
                LogUtils.i("BaseQuestionFragment", "一道题多选  ExerciseChoiceQuestionFragment");
                ExerciseChoiceQuestionFragment fragment = ExerciseChoiceQuestionFragment.newInstance(mPractiseRecord, entity);
                fragment.setOnFlingGestureListener(this);
                mCurrentFragment = fragment;
                break;
            case TYPE_CHOICE_SINGLE:
                LogUtils.i("BaseQuestionFragment", "一道题单选  ExerciseChoiceQuestionFragment");
                ExerciseChoiceQuestionFragment fragment1 = ExerciseChoiceQuestionFragment.newInstance(mPractiseRecord, entity);
                fragment1.setOnFlingGestureListener(this);
                mCurrentFragment = fragment1;
                break;
            case TYPE_MULTIPLE_CHOICE:
                LogUtils.i("BaseQuestionFragment", "一大题多道选择题  ExerciseMultipleChoiceFragment");
//                if (isNext) {
                currMultipleIndex = 0;
                mTvNext.setText("下一问");
//                } else {
//                    currMultipleIndex = entity.getQuestionObj().blocks.stems.size() - 1;
//                    mTvPrev.setText("上一问");
//                }

                ExerciseMultipleChoiceFragment fragment2 = ExerciseMultipleChoiceFragment.newInstance(mPractiseRecord, entity, currMultipleIndex);
                fragment2.setGestureFlingListener(this);
                mCurrentFragment = fragment2;
//                mCurrentFragment = ExerciseMultipleChoiceFragment.newInstance(mPractiseRecord, entity, currMultipleIndex);
                break;
            case TYPE_SUBJECTIVE:
                LogUtils.i("BaseQuestionFragment", "主观题  ExercisePhotoQuestionFragment");
                ExercisePhotoQuestionFragment fragment3 = ExercisePhotoQuestionFragment.newInstance(mPractiseRecord, entity);
                fragment3.setGestureFlingListener(this);
                mCurrentFragment = fragment3;
//                mCurrentFragment = ExercisePhotoQuestionFragment.newInstance(mPractiseRecord, entity);
                break;
        }

        transaction.replace(R.id.fl_container, mCurrentFragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void onChoice() {
        mTvNext.performClick();
    }

    public abstract void getData();

    public void showProgress(boolean isShow) {
        mProgressView.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public void showNoNetWork(boolean isShow) {
        mNoNetworkView.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {

        float x1 = e1.getX();
        float x2 = e2.getX();

        float y1 = e1.getY();
        float y2 = e2.getY();

        if (vX >= 0 && Math.abs(y2 - y1) < 150 && Math.abs(x2 - x1) >= 100) {
            showPreItem();
            LogUtils.i("BaseQuestionActivity", "向右滑动");
            return true;
        } else if (vX < 0 && Math.abs(y2 - y1) < 150 && Math.abs(x2 - x1) >= 100) {
            showNextItem();
            LogUtils.i("BaseQuestionActivity", "向左滑动");
            return true;
        }
        return false;
    }
}
