/*
 * Copyright 2015 Benedikt Vogler.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * If this software is used for a game the official „Wurfel Engine“ logo or its name must be
 *   visible in an intro screen or main menu.
 * * Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * * Neither the name of Benedikt Vogler nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.bombinggames.wurfelengine.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.bombinggames.wurfelengine.WE;
import static com.bombinggames.wurfelengine.core.Controller.getMap;
import editor.EditorView;

/**
 * The GameplayScreen State. This is state where the Wurfel Engine magic
 * happens. A controller and view is needed.
 *
 * @author Benedikt
 */
public class GameplayScreen extends WEScreen {

	private GameView view = null;
	private Controller controller = null;
	/**
	 * the view used when changed to editor mode
	 */
	private EditorView editorView;
	private boolean updateDone = true;
	private boolean updatedAtLeastOnce;

	/**
	 * Create the gameplay state. This shows the loading screen.
	 *
	 * @param controller The controller of this screen.
	 * @param view The user view of this screen.
	 */
	public GameplayScreen(final Controller controller, final GameView view) {
		this.controller = controller;
		this.view = view;
	}

	/**
	 * Get the current active view.
	 *
	 * @return
	 */
	public GameView getView() {
		return view;
	}

	/**
	 *
	 * @return
	 */
	public Controller getController() {
		return controller;
	}

	/**
	 * Set the currently used active view.
	 *
	 * @param view
	 */
	public void setView(final GameView view) {
		this.view = view;
	}

	/**
	 *
	 * @param controller
	 */
	public void setController(final Controller controller) {
		this.controller = controller;
	}

	@Override
	public void renderImpl(final float delta) {
		if (WE.getCVars().getValueB("enablemultiThreadRendering")) {
			if (updateDone) {
				updateDone = false;
				if (updatedAtLeastOnce) {
					view.getCameras().get(0).startMultiRendering();
					view.render();
				}
				new Thread(() -> {
					float avgDt = controller.getDevTools().getAverageDelta(WE.getCVars().getValueI("numFramesAverageDelta")) * 1000;

					if (avgDt >= WE.getCVars().getValueF("MaxDelta")) {
						avgDt = 1000f / 60f;//if <1 FPS assume it was stopped and set delta to 16,66ms ^= 60FPS
					}
					//apply game world speed
					float dt = avgDt * WE.getCVars().getValueF("timespeed");
					//update data
					MessageManager.getInstance().update();
					view.preUpdate(dt);
					controller.update(dt);
					Controller.staticUpdate(dt);
					view.update(dt);
					getMap().postUpdate(dt);//hack to prevent 1-frame lag by too late write access via view update

					updateDone = true;
					updatedAtLeastOnce = true;
					//Gdx.app.postRunnable(() -> {});
				}).start();
			} else {
				if (updatedAtLeastOnce) {
					view.render();
				}
			}
		} else {
			float avgDt = controller.getDevTools().getAverageDelta(WE.getCVars().getValueI("numFramesAverageDelta")) * 1000;

			if (avgDt >= WE.getCVars().getValueF("MaxDelta")) {
				avgDt = 1000f / 60f;//if <1 FPS assume it was stopped and set delta to 16,66ms ^= 60FPS
			}
			//apply game world speed
			float dt = avgDt * WE.getCVars().getValueF("timespeed");

			//update data
			MessageManager.getInstance().update();
			view.preUpdate(dt);
			controller.update(dt);
			Controller.staticUpdate(dt);
			view.update(dt);
			getMap().postUpdate(dt);//hack to prevent 1-frame lag by too late write access via view update
			view.render();
		}

		//render data
		WE.getEngineView().getStage().draw();
	}

	@Override
	public void resize(final int width, final int height) {
		Gdx.graphics.setTitle("Wurfelengine V" + WE.VERSION + " " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
		view.resize(width, height);
		HdpiUtils.glViewport(0, 0, width, height);
		//WE.getEngineView().getStage().setViewport(width, height);
	}

	@Override
	public void show() {
		WE.getEngineView().resetInputProcessors();
		GameView.classInit();
		this.controller.init();
		this.view.init(controller, null);
		controller.enter();
		view.enter();
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	
	@Override
	public void dispose() {
		controller.dispose();
		view.dispose();
		WE.SOUND.stopEverySound();
		WE.SOUND.disposeMusic();
		Controller.staticDispose();
	}

	/**
	 * lazy init
	 *
	 * @return
	 */
	public EditorView getEditorView() {
		if (editorView == null) {
			editorView = new EditorView();
		}
		return editorView;
	}

}