package common.util.pack;

import common.pack.Identifier;
import common.pack.IndexContainer;
import common.pack.PackData;
import common.util.anim.AnimU;

/**
 * 既定パックに属する悪魔魂アニメーションを、整数 ID で識別可能にする。
 */
@IndexContainer.IndexCont(PackData.class)
public class DemonSoul extends AbSoul implements IndexContainer.Indexable<PackData, DemonSoul> {

    private final Identifier<DemonSoul> id;

    public DemonSoul(int id, AnimU<?> animS) {
        super(animS);
        this.id = new Identifier<>(Identifier.DEF, DemonSoul.class, id);
    }

    @Override
    public Identifier<DemonSoul> getID() {
        return id;
    }

    @Override
    public String toString() {
        return "demonsoul_" + id;
    }
}