package common.util.pack;

import common.pack.Identifier;
import common.util.Animable;
import common.util.anim.AnimU;
import common.util.anim.EAnimI;

/**
 * 消滅時などに再生する魂アニメーションの共通基底型。
 * 識別子と表示名の決定は、既定魂・悪魔魂などの具象型に委ねる。
 */
public abstract class AbSoul extends Animable<AnimU<?>, AnimU.UType> {

    public AbSoul(AnimU<?> animS) {
        anim = animS;
    }

    abstract public Identifier<?> getID();

    abstract public String toString();

    @Override
    public EAnimI getEAnim(AnimU.UType uType) {
        return anim.getEAnim(uType);
    }
}
