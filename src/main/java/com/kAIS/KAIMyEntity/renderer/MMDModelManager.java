package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.KAIMyEntity;
import com.kAIS.KAIMyEntity.NativeFunc;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MMDModelManager {
    static Map<String, Model> models;
    static String gameDirectory = Minecraft.getInstance().gameDirectory.getAbsolutePath();

    public static void Init() {
        models = new HashMap<>();
        KAIMyEntity.logger.info("MMDModelManager.Init() finished.");
    }

    public static IMMDModel LoadModel(String modelName, long layerCount) {
        //Model path
        File modelDir = new File(gameDirectory + "/KAIMyEntity/" + modelName);
        String modelDirStr = modelDir.getAbsolutePath();

        String modelFilenameStr;
        boolean isPMD;
        File pmxModelFilename = new File(modelDir, "model.pmx");
        if (pmxModelFilename.isFile()) {
            modelFilenameStr = pmxModelFilename.getAbsolutePath();
            isPMD = false;
        } else {
            File pmdModelFilename = new File(modelDir, "model.pmd");
            if (pmdModelFilename.isFile()) {
                modelFilenameStr = pmdModelFilename.getAbsolutePath();
                isPMD = true;
            } else {
                return null;
            }
        }
        return MMDModelOpenGL.Create(modelFilenameStr, modelDirStr, isPMD, layerCount);
    }

    public static Model GetNotPlayerModel(String modelName, String animPlaying) {
        Model model = models.get(modelName + animPlaying);
        if (model == null) {
            IMMDModel m = LoadModel(modelName, 1);
            if (m == null)
                return null;
            MMDAnimManager.AddModel(m);
            AddModel(modelName + animPlaying, m, modelName, false);
            model = models.get(modelName + animPlaying);
            model.model.ChangeAnim(MMDAnimManager.GetAnimModel(model.model, animPlaying), 0);
        }
        return model;

    }

    public static Model GetPlayerModel(String modelName) {
        Model model = models.get(modelName);
        if (model == null) {
            IMMDModel m = LoadModel(modelName, 3);
            if (m == null)
                return null;
            MMDAnimManager.AddModel(m);
            AddModel(modelName, m, modelName, true);
            model = models.get(modelName);
        }
        return model;

    }

    public static void AddModel(String Name, IMMDModel model, String modelName, boolean isPlayer) {
        if (isPlayer) {
            NativeFunc nf = NativeFunc.GetInst();
            PlayerData pd = new PlayerData();
            pd.stateLayers = new PlayerData.EntityState[3];
            pd.playCustomAnim = false;
            pd.rightHandMat = nf.CreateMat();
            pd.leftHandMat = nf.CreateMat();
            pd.matBuffer = ByteBuffer.allocateDirect(64); //float * 16

            ModelWithPlayerData m = new ModelWithPlayerData();
            m.entityName = Name;
            m.model = model;
            m.modelName = modelName;
            m.playerData = pd;
            model.ResetPhysics();
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
            models.put(Name, m);
        } else {
            ModelWithEntityState m = new ModelWithEntityState();
            m.entityName = Name;
            m.model = model;
            m.modelName = modelName;
            m.state = MMDModelManager.EntityState.Idle;
            model.ResetPhysics();
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
            models.put(Name, m);
        }
    }

    public static void ReloadModel() {
        for (Model i : models.values())
            DeleteModel(i);
        models = new HashMap<>();
    }

    static void DeleteModel(Model model) {
        MMDModelOpenGL.Delete((MMDModelOpenGL) model.model);

        //Unregister animation user
        MMDAnimManager.DeleteModel(model.model);
    }

    enum EntityState {Idle, Walk, Swim, Ridden, Driven, Sleep}

    static class ModelWithEntityState extends Model {
        EntityState state;
    }

    public static class Model {
        public IMMDModel model;
        String entityName;
        String modelName;
        public Properties properties = new Properties();
        boolean loadedProperties = false;

        public void loadModelProperties(boolean forceReload){
            if (loadedProperties && !forceReload)
                return;
            String path2Properties = gameDirectory + "/KAIMyEntity/" + modelName + "/model.properties";
            try {
                InputStream istream = new FileInputStream(path2Properties);
                properties.load(istream);
            } catch (IOException e) {
                KAIMyEntity.logger.warn( "KAIMyEntity/" + modelName + "/model.properties not found" );
            }
            loadedProperties = true;
        } 
    }

    public static class ModelWithPlayerData extends Model {
        public PlayerData playerData;
    }

    public static class PlayerData {
        public static HashMap<EntityState, String> stateProperty = new HashMap<>() {{
            put(EntityState.Idle, "idle");
            put(EntityState.Walk, "walk");
            put(EntityState.Sprint, "sprint");
            put(EntityState.Air, "air");
            put(EntityState.OnClimbable, "onClimbable");
            put(EntityState.OnClimbableUp, "onClimbableUp");
            put(EntityState.OnClimbableDown, "onClimbableDown");
            put(EntityState.Swim, "swim");
            put(EntityState.Ride, "ride");
            put(EntityState.Sleep, "sleep");
            put(EntityState.ElytraFly, "elytraFly");
            put(EntityState.Die, "die");
            put(EntityState.SwingRight, "swingRight");
            put(EntityState.SwingLeft, "swingLeft");
            put(EntityState.Sneak, "sneak");
            put(EntityState.OnHorse, "onHorse");
            put(EntityState.Crawl, "crawl");
            put(EntityState.LieDown, "lieDown");
        }};
        public boolean playCustomAnim; //Custom animation played in layer 0.
        public long rightHandMat, leftHandMat;
        public EntityState[] stateLayers;
        ByteBuffer matBuffer;

        public enum EntityState {Idle, Walk, Sprint, Air, OnClimbable, OnClimbableUp, OnClimbableDown, Swim, Ride, Sleep, ElytraFly, Die, SwingRight, SwingLeft, ItemRight, ItemLeft, Sneak, OnHorse, Crawl, LieDown}
    }
}