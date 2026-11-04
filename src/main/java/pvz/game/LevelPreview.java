package pvz.game;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import pvz.level.Level;
import pvz.level.LevelLoader;
import pvz.level.LevelScreenshot;
import java.io.IOException;
import java.nio.file.Path;
import pvz.plant.Cards;
import pvz.plant.Plant;
import pvz.plant.PlantCatalog;
import pvz.world.Assets;
import pvz.world.Car;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieSpawn;

/** 用关卡数据和现有绘图器制作战场封面，不生成磁盘文件或启动另一局游戏 */
public class LevelPreview {
    private static final long PREVIEW_TIME = 1000;
    private static final int MAX_ZOMBIES = 5;
    private final Assets assets;

    /** 参数 assets 是当前游戏共用的素材，避免为每张封面重新加载动画 */
    public LevelPreview(Assets assets) {
        this.assets = assets;
    }

    /** 参数 number 是关卡编号，返回按真实战场布局绘制的封面 */
    public BufferedImage create(int number) {
        LevelLoader loader = new LevelLoader(assets);
        Level level = loader.load(number);
        if (!level.screenshot.isEmpty()) {
            try {
                Path levelPath = assets.levelPath(number);
                Path levelsDirectory = levelPath.getParent();
                Path root = levelsDirectory.getParent();
                BufferedImage screenshot = LevelScreenshot.read(root, level.screenshot);
                return LevelScreenshot.fit(screenshot, Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT);
            } catch (IOException | IllegalArgumentException exception) {
                // 截图丢失不能挡住游戏，报告原因后继续使用原来的自动封面
                System.out.println("关卡截图读取失败，改用自动封面：" + exception.getMessage());
            }
        }
        GameState preview = new GameState();
        preview.screen = GameScreen.PLAY;
        preview.time = PREVIEW_TIME;
        preview.backgroundIndex = level.backgroundIndex;
        preview.background = assets.frame("Background", level.backgroundIndex);
        preview.sunValue = level.initialSun;
        preview.barType = level.barType;
        addPlants(preview, level);
        addZombies(preview, level);
        for (int row = 0; row < Layout.ROW_COUNT; row = row + 1) {
            preview.cars.add(new Car(row));
        }
        BufferedImage image = new BufferedImage(Layout.WINDOW_WIDTH,
            Layout.WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = image.createGraphics();
        GameRenderer renderer = new GameRenderer(assets);
        renderer.draw(painter, GameScreen.PLAY, PREVIEW_TIME, preview, false);
        painter.dispose();
        return image;
    }

    /** 参数 state 是封面状态，level 提供可选植物；只摆可用植物，保持封面与关卡匹配 */
    private void addPlants(GameState state, Level level) {
        List<Integer> choices = new ArrayList<Integer>();
        if (level.barType != Layout.BAR_NORMAL) {
            choices.addAll(level.cardPool);
        } else {
            choices.addAll(level.requiredPlants);
            for (int index = 0; index < PlantCatalog.CHOOSER_COUNT; index = index + 1) {
                if (choices.size() >= level.maxCards) {
                    break;
                }
                if (!level.bannedPlants.contains(index) && !choices.contains(index)) {
                    choices.add(index);
                }
            }
        }
        if (level.barType == Layout.BAR_NORMAL) {
            state.cards.addAll(Cards.staticBar(choices));
        }
        if (choices.isEmpty()) {
            return;
        }
        for (int row = 0; row < Layout.ROW_COUNT; row = row + 1) {
            int choicePosition = row % choices.size();
            int choice = choices.get(choicePosition);
            String name = PlantCatalog.nameAt(choice);
            int column = row % 2;
            Plant plant = new Plant(name, Layout.columnCenter(column), Layout.rowBottom(row),
                row, column, assets, PREVIEW_TIME, level.backgroundIndex == 0);
            state.plants.add(plant);
        }
    }

    /** 参数 state 是封面状态，level 提供实际敌人；每个品种最多摆一只以便识别 */
    private void addZombies(GameState state, Level level) {
        List<String> kinds = new ArrayList<String>();
        for (ZombieSpawn spawn : level.spawns) {
            if (kinds.contains(spawn.name)) {
                continue;
            }
            int row = kinds.size() % Layout.ROW_COUNT;
            kinds.add(spawn.name);
            Zombie zombie = new Zombie(spawn.name, row, Layout.rowBottom(row), assets);
            Rectangle body = zombie.collisionBox(assets, PREVIEW_TIME);
            int center = Layout.columnCenter(6 + row % 2);
            zombie.x = zombie.x + center - body.getCenterX();
            zombie.animationStart = PREVIEW_TIME;
            state.zombies.add(zombie);
            if (kinds.size() >= MAX_ZOMBIES) {
                break;
            }
        }
    }
}
