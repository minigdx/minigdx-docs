package com.github.minigdx.docs.camera

import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.relation.ObjectType
import com.github.dwursteisen.minigdx.GL
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.GameScreen
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Camera
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.events.Event
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.Texture
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.graphics.GLResourceClient
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.render.RenderStage
import com.github.minigdx.docs.quick.start.Cube
import com.github.minigdx.imgui.WidgetBuilder
import your.game.ImGUIRenderStage
import your.game.ImGuiSystem

class ChangeCameraType : Event

class CameraTypeSystem(private val gameScreen: GameScreen) : System(EntityQuery.of(Camera::class)) {

    override fun update(delta: Seconds, entity: Entity) = Unit

    override fun update(delta: Seconds) {
        if (input.isKeyJustPressed(Key.SPACE)) {
            emit(ChangeCameraType())
        }
    }

    private val perspective = Camera(
        gameScreen,
        Camera.Type.PERSPECTIVE,
        far = 100f,
        near = 0.1f,
        fov = 25f,
        scale = 0.0f
    )

    private val orthographic = Camera(
        gameScreen,
        Camera.Type.ORTHOGRAPHIC,
        far = 100f,
        near = 0.1f,
        scale = 15f
    )

    override fun onEvent(event: Event, entityQuery: EntityQuery?) = when (event) {
        is ChangeCameraType -> entities.forEach { entity ->
            val camera = entity.get(Camera::class)
            val cameraType = camera.type

            when (cameraType) {
                Camera.Type.PERSPECTIVE -> {
                    entity.remove(camera)
                    entity.add(orthographic)
                }
                Camera.Type.ORTHOGRAPHIC -> {
                    entity.remove(camera)
                    entity.add(perspective)
                }
            }
        }
        else -> Unit
    }
}

class CameraGUI : ImGuiSystem() {

    private val cameras by interested(EntityQuery.Companion.of(Camera::class))

    override fun gui(builder: WidgetBuilder) {
        builder.verticalContainer(width = 0.5f) {
            cameras.forEach {
                button(label = it.get(Camera::class).type.toString()) {
                    emit(ChangeCameraType())
                }
            }
        }
    }
}

class CameraTypeGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<Scene>("cube.protobuf")

    private val texture by gameContext.fileHandler.get<Texture>("internal/widgets.png")

    override fun createEntities(entityFactory: EntityFactory) {
        scene.children.forEach { node ->
            val entity = entityFactory.createFromNode(node, scene)

            if (node.type == ObjectType.MODEL) {
                entity.add(Cube())
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(CameraTypeSystem(gameContext.gameScreen), CameraGUI())
    }

    override fun createRenderStage(gl: GL, compiler: GLResourceClient): List<RenderStage<*, *>> {
        // TODO: The stage renderer should be able do load the default texture by itself?
        return super.createRenderStage(gl, compiler) + ImGUIRenderStage(gl, compiler, texture, gameContext)
    }
}
