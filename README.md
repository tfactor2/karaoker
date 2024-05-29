
### Install mvn

```bash
./mvnw clean install -DskipTests
```

### Run the service 

```bash
curl -X POST -H "Content-Type: application/json" -d '{"url": "https://www.youtube.com/watch?v=4-43lLKaqBQ"}' http://localhost:8080/videos
```

Install Components

Install pyenv 
```bash 
brew install pyenv
```

Install virtual envs:
```bash 
brew install pyenv-virtualenv
```

Add to .zshrc or profile:
```bash
# Configure shell for pyenv and pyenv-virtualenv
echo 'export PYENV_ROOT="$HOME/.pyenv"' >> ~/.zshrc
echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> ~/.zshrc
echo 'eval "$(pyenv init --path)"' >> ~/.zprofile
echo 'eval "$(pyenv init -)"' >> ~/.zshrc
echo 'eval "$(pyenv virtualenv-init -)"' >> ~/.zshrc
exec "$SHELL"
```

Install WORKABLE for spleeter version of python
```bash
pyenv install 3.9.19
```

Create a virual env `my_env` for the pyenv 
```bash 
pyenv virtualenv 3.9.19 spleeter-env
pyenv activate spleeter-env
```


# Install Spleeter
```bash
pip install spleeter
```

# Verify the installation
```bash
spleeter --help
```

# Deactivate the virtual environment
```bash
pyenv deactivate
```

pip install numpy==1.20.0