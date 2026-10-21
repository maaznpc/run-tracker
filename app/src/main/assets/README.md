Run `python3 scripts/fetch_model.py` from the project root to place Google's official
pose_landmarker_lite.task here. Android builds fail explicitly when it is absent.
The model could not be downloaded in the delivery environment because its network
proxy refused connections. The application never downloads it at runtime.
