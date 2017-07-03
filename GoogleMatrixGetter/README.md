# GoogleMatrixGetter

This part is intended to be used to generate distance matrices for real locations. Based on two services: Yandex Geocoding to map real addresses to their world coordinates (Yandex is better for locations in Russia) and Google Maps Api to build the distace and time matrix itself based on the coordinates.

# Usage

- Save your `yandex` and `google` API keys to the `y_api.key` and `g_api.key`
- `pip install -r requirements.txt`
- `python main.py` (*Have a look at the code and the files it uses*)