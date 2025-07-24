# Makefile

install:
	pip install -r requirements.txt

test:
	python -m pytest

virtual-win:
	py -m venv venv

virtual-linux:
	python3 -m venv venv
	