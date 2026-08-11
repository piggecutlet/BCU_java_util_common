package common.util.stage.info;

import common.util.stage.Stage;

/**
 * ステージ定義に付随する報酬とEXステージ接続情報の共通参照契約。
 * 公式データとユーザーパックでは未対応項目の戻り値が異なるため、実装種別を前提に解釈する。
 */
public interface StageInfo {
    boolean hasExConnection();

    Stage[] getExStages();

    /**
     * EXステージ候補ごとの抽選率。
     */
    float[] getExChances();

    /**
     * いずれかのEXステージへ接続する確率。
     */
    int getExChance();

    int getExMapId();

    int getExStageIdMin();

    int getExStageIdMax();

    Stage getStage();

    int getEnergy();

    int getXp();

    int[][] getDrop();

    int[][] getTime();
}

