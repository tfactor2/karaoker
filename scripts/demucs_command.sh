#!/bin/bash

# Ensure pyenv is initialized (modify the path to pyenv.sh as necessary)
export PYENV_ROOT="$HOME/.pyenv"
export PATH="$PYENV_ROOT/bin:$PATH"
eval "$(pyenv init --path)"
eval "$(pyenv init -)"
eval "$(pyenv virtualenv-init -)"

# Activate the specific virtual environment
pyenv local 3.9.19
pyenv virtualenv 3.9.19 demucs-env
pyenv activate demucs-env

# Run the demucs command
demucs "$@"