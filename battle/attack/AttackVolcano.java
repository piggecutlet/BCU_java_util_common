package common.battle.attack;

import common.battle.entity.AbEntity;
import common.battle.entity.Entity;
import common.util.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 烈波・小烈波の持続攻撃判定。
 * 一定間隔で既命中集合を消去し、各間隔につき同じ実体へ1回だけダメージを与える。
 */
public class AttackVolcano extends AttackAb {
	public ContVolcano handler;
	protected boolean attacked = false;
	protected final List<Entity> vcapt = new ArrayList<>();

	private byte volcTime = VOLC_ITV;

	public AttackVolcano(Entity e, AttackAb a, float sta, float end, int vt) {
		super(e, a, sta, end, false);
		isCounter = a.isCounter;
		this.sta = sta;
		this.end = end;
		this.waveType = vt;
	}

	@Override
	public void capture() {
		List<AbEntity> le = model.b.inRange(touch, -dire, sta, end, excludeRightEdge);

		capt.clear();

		for (AbEntity e : le)
			if (e instanceof Entity && !vcapt.contains((Entity) e)) {
				capt.add(e);
			}
	}

	@Override
	public void excuse() {
		process();

		volcTime--;
		if (volcTime == 0) {
			volcTime = VOLC_ITV;
			vcapt.clear();
		}

		atk = rawAtk;

		if (attacker != null) {
			int[][] status = attacker.status;
			if (status[P_STRONG][0] != 0)
				atk += atk * status[P_STRONG][0] / 100;
			if (status[P_STRONG][1] != 0)
				atk += atk * status[P_STRONG][1] / 100;
			if (status[P_WEAK][0] != 0)
				atk = atk * status[P_WEAK][1] / 100;
			if (attacker.dire == 1 && attacker.basis.canon.deco == DECO_BASE_WATER)
				atk = (int) (atk * attacker.basis.b.t().getDecorationMagnification(attacker.basis.canon.deco, Data.DECO_SURGE));
		}

		for (AbEntity e : capt) {
			if (e.isBase() && !(e instanceof Entity))
				continue;

			if (e instanceof Entity) {
				boolean damaged = e.damaged(this);
				if (damaged)
					((Entity) e).lastHitBy.add(this);
				attacked = true;
				vcapt.add((Entity) e);
			}
		}
	}
}
