from datetime import datetime

from pydantic import BaseModel, PositiveInt, ConfigDict


class UserCreate(BaseModel):
    username: str
    password: str

class User(BaseModel):
    user_id: PositiveInt
    username: str
    is_disabled: bool = False
    avatar_file_id: PositiveInt | None = None
    last_seen: datetime = datetime.now()
    is_admin: bool = False
    model_config = ConfigDict(extra="ignore", from_attributes=True)

class RegisterUser(BaseModel):
    username: str
    is_disabled: bool = False
    avatar_file_id: PositiveInt | None = None
    last_seen: datetime = datetime.now()
    is_admin: bool = False
    hashed_password: str

    model_config = ConfigDict(extra="ignore", from_attributes=True)

class UserInDb(User):
    hashed_password: str