package common.util.unit;

/**
 * 体力倍率と攻撃倍率を別々に受け渡すための値。
 * 現在レベルや戦闘中の能力値は保持しない。
 */
public class Magnification implements LevelInterface {
    public int hp;
    public int atk;

    public Magnification(int hp, int atk) {
        this.hp = hp;
        this.atk = atk;
    }
}
