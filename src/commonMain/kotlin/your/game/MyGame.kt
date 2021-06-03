package your.game

import com.curiouscreature.kotlin.math.Mat4
import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.relation.ObjectType
import com.github.dwursteisen.minigdx.GL
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.GameScreen
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Color
import com.github.dwursteisen.minigdx.ecs.components.LightComponent
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.Texture
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.graphics.GLResourceClient
import com.github.dwursteisen.minigdx.input.InputHandler
import com.github.dwursteisen.minigdx.input.TouchSignal
import com.github.dwursteisen.minigdx.math.Vector2
import com.github.dwursteisen.minigdx.render.RenderStage
import com.github.dwursteisen.minigdx.shaders.DataSource
import com.github.dwursteisen.minigdx.shaders.ShaderProgram
import com.github.dwursteisen.minigdx.shaders.fragment.UVFragmentShader
import com.github.dwursteisen.minigdx.shaders.vertex.MeshVertexShader
import com.github.minigdx.imgui.ImGUIRenderer
import com.github.minigdx.imgui.InputCapture
import com.github.minigdx.imgui.internal.Resolution
import com.github.minigdx.imgui.gui
import com.github.minigdx.docs.quick.start.Cube
import com.github.minigdx.docs.quick.start.RotatingCubeSystem

class MyGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<Scene>("cube.protobuf")

    private val texture by gameContext.fileHandler.get<Texture>("internal/widgets.png")

    override val clearColor: Color = Color(0.5f, 0.5f)

    override fun createEntities(entityFactory: EntityFactory) {
        // Create all entities needed at startup
        // The scene is the node graph that can be updated in Blender
        scene.children.forEach { node ->
            // Create an entity using all information from this node (model, position, camera, ...)
            val entity = entityFactory.createFromNode(node, scene)

            // The node is the cube from the Blender file
            if (node.type == ObjectType.MODEL) {
                // Mark this entity as being a cube
                entity.add(Cube())
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        // Create all systems used by the game
        return listOf(RotatingCubeSystem())
    }

    override fun createRenderStage(gl: GL, compiler: GLResourceClient): List<RenderStage<*, *>> {
        return super.createRenderStage(gl, compiler) + object : RenderStage<MeshVertexShader, UVFragmentShader>(
            gl,
            compiler,
            MeshVertexShader(),
            UVFragmentShader(),
            EntityQuery.none(),
            EntityQuery.none()
        ) {

            private val guiRenderer = ImGUI(gl, { program }, vertex, fragment, gameContext.gameScreen, texture)

            private val inputCapture: InputCapture = ImgCapture({input})

            override fun update(delta: Seconds) {
                gl.useProgram(program)
                gui(
                    renderer = guiRenderer,
                    inputCapture = inputCapture,
                    gameResolution = Resolution(gameContext.gameScreen.width, gameContext.gameScreen.height)
                ) {
                    verticalContainer(width = 0.25f) {
                        button(label = "Hello World") {
                            println("click 1")
                        }
                        button(label = "Hello World")  {
                            println("click 2")
                        }
                    }
                }
            }

            override fun update(delta: Seconds, entity: Entity) = Unit
        }
    }
}

class ImgCapture(val input: () -> InputHandler) : InputCapture {

    private val pos: Vector2 = Vector2(-1f, -1f)
    private var touch = false

    override val x: Float
        get() = pos.x
    override val y: Float
        get() = pos.y
    override val isTouch: Boolean
        get() = touch

    override fun update() {
        val inputHandler = input()
        inputHandler.touchIdlePosition()?.run {
            pos.x = this.x
            pos.y = this.y
        }

        touch = false
        inputHandler.isJustTouched(TouchSignal.TOUCH1)?.run {
            pos.x = this.x
            pos.y = this.y
            touch = true
        }
    }
}

class ImGUI(
    private val gl: GL,
    private val programFactory: () -> ShaderProgram,
    private val vertex: MeshVertexShader,
    private val fragmentShader: UVFragmentShader,
    private val gameScreen: GameScreen,
    private val texture: Texture
) : ImGUIRenderer {

    private val verticesBuffer = gl.createBuffer()
    private val verticesOrderBuffer = gl.createBuffer()
    private val verticesUVsBuffer = gl.createBuffer()
    private val textureBuffer = gl.createTexture()
    private val normalsBuffer = gl.createBuffer()

    override fun render(vertices: FloatArray, uv: FloatArray, verticesOrder: IntArray) {

        gl.disable(GL.DEPTH_TEST)
        gl.enable(GL.BLEND)
        gl.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA)

        gl.bindBuffer(GL.ARRAY_BUFFER, verticesBuffer)
        gl.bufferData(
            target = GL.ARRAY_BUFFER,
            data = DataSource.FloatDataSource(vertices),
            usage = GL.STATIC_DRAW
        )

        gl.bindBuffer(GL.ARRAY_BUFFER, normalsBuffer)
        gl.bufferData(
            target = GL.ARRAY_BUFFER,
            data = DataSource.FloatDataSource(FloatArray(vertices.size)),
            usage = GL.STATIC_DRAW
        )

        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, verticesOrderBuffer)
        gl.bufferData(
            target = GL.ELEMENT_ARRAY_BUFFER,
            data = DataSource.ShortDataSource(verticesOrder.map { it.toShort() }.toShortArray()),
            usage = GL.STATIC_DRAW
        )

        gl.bindBuffer(GL.ARRAY_BUFFER, verticesUVsBuffer)
        gl.bufferData(
            target = GL.ARRAY_BUFFER,
            data = DataSource.FloatDataSource(uv),
            usage = GL.STATIC_DRAW
        )

        // Push the texture
        gl.bindTexture(GL.TEXTURE_2D, textureBuffer)

        gl.texParameteri(
            GL.TEXTURE_2D,
            GL.TEXTURE_MAG_FILTER,
            // TODO: this parameter should be configurable at the game level.
            //  Maybe add a config object in the GameContext with fields and an extra as Map
            //  for custom parameters
            GL.NEAREST
        )
        gl.texParameteri(
            GL.TEXTURE_2D,
            GL.TEXTURE_MIN_FILTER,
            GL.NEAREST
        )

        gl.texImage2D(
            GL.TEXTURE_2D,
            0,
            GL.RGBA,
            GL.RGBA,
            GL.UNSIGNED_BYTE,
            texture.source
        )

        // ---- shader configuration ---- //

        // Configure the light.

        val program = programFactory()
        vertex.uLightColor.apply(program, LightComponent.TRANSPARENT_COLOR)
        vertex.uLightPosition.apply(program, LightComponent.ORIGIN)


        vertex.uModelView.apply(program, Mat4.identity())
        vertex.aVertexPosition.apply(program, verticesBuffer)
        vertex.aVertexNormal.apply(program, normalsBuffer)
        vertex.aUVPosition.apply(program, verticesUVsBuffer)
        fragmentShader.uUV.apply(program, textureBuffer, unit = 0)

        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, verticesOrderBuffer)
        gl.drawElements(
            GL.TRIANGLES,
            verticesOrder.size,
            GL.UNSIGNED_SHORT,
            0
        )

        gl.disable(GL.BLEND)
        gl.enable(GL.DEPTH_TEST)
    }
}
