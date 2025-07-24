from datetime import datetime
from pydantic import BaseModel, ConfigDict
from pydantic import PositiveInt


MAX_FILE_SIZE = 30 * 1024 * 1024

class FileInDb(BaseModel):
    file_id: PositiveInt
    user_id: PositiveInt
    uploaded_at: datetime
    filename: str
    content_type: str
    data: bytes

    model_config = ConfigDict(from_attributes=True)

class FileMeta(BaseModel):
    file_id: PositiveInt
    user_id: PositiveInt
    filename: str
    content_type: str
    uploaded_at: datetime

    model_config = ConfigDict(extra="ignore", from_attributes=True)
