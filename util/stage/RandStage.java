package common.util.stage;

import common.CommonStatic;
import common.CommonStatic.BCAuxAssets;
import common.battle.BasisLU;
import common.battle.BasisSet;
import common.system.files.VFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * 公式のランダムダンジョン定義から編成またはステージを抽選するユーティリティ。
 * 抽選結果そのものや戦闘中の進行状態は保持しない。
 */
public class RandStage {

	public static BasisLU getLU(int att) {
		BCAuxAssets aux = CommonStatic.getBCAssets();
		double r = Math.random() * 100;
		for (int i = 0; i < 10; i++)
			if (r < aux.randRep[att][i])
				return BasisSet.current().sele.randomize(10 - i);
			else
				r -= aux.randRep[att][i];
		return BasisSet.current().sele;
	}

	public static Stage getStage(MapColc map, int sta) {
		List<Stage> l = new ArrayList<>(map.maps.get(sta).list.getList());
		return l.get((int) (Math.random() * l.size()));
	}

	public static Stage getStage(StageMap sm) {
		List<Stage> l = new ArrayList<>(sm.list.getList());
		return l.get((int) (Math.random() * l.size()));
	}

	public static void read() {
		Queue<String> qs = VFile.readLine("./org/stage/D/RandomDungeon_000.csv");
		for (int i = 0; i < 5; i++)
			CommonStatic.getBCAssets().randRep[i] = CommonStatic.parseIntsN(qs.poll());
	}

}
