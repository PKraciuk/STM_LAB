import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson


data class GameData(
    val paddle1X: Float,
    val paddle1Y: Float,
    val ballX: Float,
    val ballY: Float,
    val scorePlayer1: Int,
    val scorePlayer2: Int

)
{
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): GameData = Gson().fromJson(json, GameData::class.java)
    }
}


class DrawView(context: Context) : View(context) {
    private var width = 1.0f
    private var height = 1.0f
    private var paint: Paint = Paint()
    private var ballX = 200f
    private var ballY = 200f
    private var paddle1X = 100f
    private var paddle1Y = 100f
    private var paddle2X = 300f
    private var paddle2Y = 400f
    private var paddleWidth = 300f
    private var paddleHeight = 40f

    private var ballSpeedX = 5f
    private var ballSpeedY = 5f
    private var ballAcceleration = 0.1f


    private var scorePlayer1 = 0
    private var scorePlayer2 = 0

    init {
        // Pozostała inicjalizacja
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Constants for design dimensions (based on your original design)
        val DESIGN_WIDTH = 1440f  // Replace with your design's width
        val DESIGN_HEIGHT = 3120f // Replace with your design's height

        // Calculate scale factors for width and height
        val scaleFactorX = w / DESIGN_WIDTH
        val scaleFactorY = h / DESIGN_HEIGHT

        // Update screen dimensions
        width = w.toFloat()
        height = h.toFloat()

        // Scale the dimensions of the paddles and ball
        paddleWidth *= scaleFactorX
        paddleHeight *= scaleFactorY
        ballX = width / 2
        ballY = height / 2

        // Adjust the positions of the paddles
        paddle1Y = height - 300 * scaleFactorY
        paddle2Y = 300 * scaleFactorY
        paddle1X = width / 2 - paddleWidth / 2
        paddle2X = width / 2 - paddleWidth / 2

        // Reset scores
        scorePlayer1 = 0
        scorePlayer2 = 0

        // If you have other elements that need scaling, adjust them here
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Rysowanie tła
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Ustawienie koloru na biały dla paletki i piłki
        paint.color = Color.WHITE

        // Rysowanie paletki 1
        canvas.drawRect(paddle1X, paddle1Y, paddle1X + paddleWidth, paddle1Y + paddleHeight, paint)

        // Rysowanie paletki 2
        canvas.drawRect(paddle2X, paddle2Y, paddle2X + paddleWidth, paddle2Y + paddleHeight, paint)


        // Aktualizacja pozycji piłki
        ballX += ballSpeedX
        ballY += ballSpeedY

        // Przyspieszenie piłki
        ballSpeedX += ballAcceleration
        ballSpeedY += ballAcceleration


        if (ballX + 20 >= paddle2X && ballX - 20 <= paddle2X + paddleWidth && ballY + 20 >= paddle2Y && ballY - 20 <= paddle2Y + paddleHeight) {
            if (ballSpeedY < 0) {
                ballSpeedY = -ballSpeedY // Odbicie piłki
                ballAcceleration = -ballAcceleration
                ballSpeedY *= 0.8f

                // Adjust ballSpeedX based on paddle hit location
                val paddleCenter = paddle2X + paddleWidth / 2
                val hitPosition = ballX - paddleCenter
                ballSpeedX += hitPosition / paddleWidth * 2 // Adjust the multiplier as needed
            }
        }


        if (ballX - 20 <= paddle1X + paddleWidth && ballX + 20 >= paddle1X && ballY - 20 <= paddle1Y + paddleHeight && ballY + 20 >= paddle1Y) {
            if (ballSpeedY > 0) {
                ballSpeedY = -ballSpeedY // Odbicie piłki
                ballAcceleration = -ballAcceleration
                ballSpeedY *= 0.8f

                // Adjust ballSpeedX based on paddle hit location
                val paddleCenter = paddle1X + paddleWidth / 2
                val hitPosition = ballX - paddleCenter
                ballSpeedX += hitPosition / paddleWidth * 2 // Adjust the multiplier as needed
            }
        }


        // Detekcja kolizji z bocznymi krawędziami ekranu
        if (ballX <= 20 || ballX >= width - 20) {
            ballSpeedX = -ballSpeedX
        }

        // Resetowanie piłki po opuszczeniu pola gry
        if (ballY <= 20 ) {
            ballX = width / 2
            ballY = height / 2
            ballSpeedX = 5f
            ballSpeedY = 5f
            ballAcceleration = 0.1f
            updateScore(2)
        }

        if (ballY >= height - 20) {
            ballX = width / 2
            ballY = height / 2
            ballSpeedX = 5f
            ballSpeedY = 5f
            ballAcceleration = 0.1f
            updateScore(1)
        }

        // Rysowanie piłki
        canvas.drawCircle(ballX, ballY, 20f, paint) // 10f to promień piłki

        val alpha = 128 // 50% transparent. Range is from 0 (transparent) to 255 (opaque)

        // Set the color for the score with alpha
        paint.setARGB(alpha, 255, 255, 255) // Assuming white color with 50% opacity



        paint.textSize = 300f // Set the text size for the score

        canvas.drawText("$scorePlayer2", width/2 - 90, (height/3)*2, paint) // Position for player 2's score
        canvas.save() // Zapisz aktualny stan Canvas
        canvas.rotate(180f, width / 2f, height / 2f) // Obróć wokół środka ekranu


        canvas.drawText("$scorePlayer1", width/2 - 90, (height/3)*2, paint) // Position for player 1's score

        canvas.restore() // Przywróć stan Canvas do pierwotnego


        // Wywołanie invalidate() spowoduje ponowne wywołanie onDraw
        invalidate()
    }

    private fun updateScore(player: Int) {
        if (player == 1) {
            scorePlayer1++
        } else if (player == 2) {
            scorePlayer2++
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(index)
                val y = event.getY(index)

                // Rozpocznij sterowanie paletką
                updatePaddlePosition(x, y, pointerId)
            }
            MotionEvent.ACTION_MOVE -> {
                // Uaktualnij wszystkie aktywne punkty dotyku
                for (i in 0 until event.pointerCount) {
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val id = event.getPointerId(i)

                    updatePaddlePosition(x, y, id)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Zakończ sterowanie paletką
                // Możesz tutaj zaimplementować dodatkową logikę
            }
        }
        invalidate()
        return true
    }

    private fun updatePaddlePosition(x: Float, y: Float, pointerId: Int) {
        // Sprawdzanie, która część ekranu została dotknięta
        val isTopSide = y > height / 2

        if (isTopSide) {
            // Przesuń górną paletkę
            paddle1X = x - paddleWidth / 2
            paddle1X = Math.max(0f, Math.min(paddle1X, width - paddleWidth))
        }
    }

    fun prepareGameData(): GameData {
        // Capture the current state of the game
        return GameData(
            paddle1X = paddle1X / width,   // Store as relative position
            paddle1Y = paddle1Y / height,  // Store as relative position
            ballX = ballX / width,         // Store as relative position
            ballY = ballY / height,        // Store as relative position
            scorePlayer1 = scorePlayer1,
            scorePlayer2 = scorePlayer2
        )
    }

    fun updateGameState(gameData: GameData) {
        // Convert relative positions back to pixel values based on local screen size
        this.ballX = width - (gameData.ballX * width) // gameData.ballX is a relative value (0 to 1)
        this.ballY = height - (gameData.ballY * height)
        this.paddle2X = width - (gameData.paddle1X * width) - paddleWidth
        this.paddle2Y = height - (gameData.paddle1Y * height)
        this.scorePlayer1 = gameData.scorePlayer2
        this.scorePlayer2 = gameData.scorePlayer1
        invalidate() // Redraw the view with updated data
    }

    fun setEnemyPaddle(relativeX: Float) {
        // Convert the relative position to a pixel value
        this.paddle2X = relativeX * width
        invalidate() // Redraw the view with updated data
    }


    fun getPaddleX(): Float {
        return paddle1X/width

    }


}
